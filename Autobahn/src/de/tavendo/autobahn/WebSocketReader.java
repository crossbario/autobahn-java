/******************************************************************************
 *
 *  Copyright 2011-2012 Tavendo GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package de.tavendo.autobahn;

import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

/**
 * WebSocket reader, the receiving leg of a WebSockets connection.
 * This runs on it's own background thread and posts messages to master
 * thread's message queue for there to be consumed by the application.
 * The only method that needs to be called (from foreground thread) is quit(),
 * which gracefully shuts down the background receiver thread.
 */
public class WebSocketReader extends Thread {

   private static final boolean DEBUG = true;
   private static final String TAG = WebSocketReader.class.getName();

   private final Handler mMaster;
   private final SocketChannel mSocket;
   private final WebSocketOptions mOptions;
   private final SSLEngine mSSLEngine;

   private final ByteBuffer mBuffer;
   private final ByteBuffer mBufferEnc;
   private NoCopyByteArrayOutputStream mMessagePayload;

   private final static int STATE_CLOSED = 0;
   private final static int STATE_CONNECTING = 1;
   private final static int STATE_CLOSING = 2;
   private final static int STATE_OPEN = 3;

   private boolean mStopped = false;
   private int mState;

   private boolean mInsideMessage = false;
   private int mMessageOpcode;

   /// Frame currently being received.
   private FrameHeader mFrameHeader;

   private Utf8Validator mUtf8Validator = new Utf8Validator();


   /**
    * WebSockets frame metadata.
    */
   private static class FrameHeader {
      public int mOpcode;
      public boolean mFin;
      @SuppressWarnings("unused")
      public int mReserved;
      public int mHeaderLen;
      public int mPayloadLen;
      public int mTotalLen;
      public byte[] mMask;
   }


   /**
    * Create new WebSockets background reader.
    *
    * @param master    The message handler of master (foreground thread).
    * @param socket    The socket channel created on foreground thread.
    * @param mSSLEngine 
    */
   public WebSocketReader(Handler master, SocketChannel socket, WebSocketOptions options, String threadName, SSLEngine sslengine) {

      super(threadName);

      mMaster = master;
      mSocket = socket;
      mOptions = options;
      mSSLEngine = sslengine;

      if (mSSLEngine != null) {
         mBufferEnc = ByteBuffer.allocateDirect(options.getMaxFramePayloadSize() + 14 + 100);
         mBuffer = ByteBuffer.allocate(options.getMaxFramePayloadSize() + 14);
      } else {
         mBufferEnc = null;
         mBuffer = ByteBuffer.allocateDirect(options.getMaxFramePayloadSize() + 14);
      }

      mMessagePayload = new NoCopyByteArrayOutputStream(options.getMaxMessagePayloadSize());

      mFrameHeader = null;
      mState = STATE_CONNECTING;

      if (DEBUG) Log.d(TAG, "created");
   }


   /**
    * Graceful shutdown of background reader thread (called from master).
    */
   public void quit() {

      mStopped = true;

      if (DEBUG) Log.d(TAG, "quit");
   }


   /**
    * Notify the master (foreground thread) of WebSockets message received
    * and unwrapped.
    *
    * @param message       Message to send to master.
    */
   protected void notify(Object message) {

      Message msg = mMaster.obtainMessage();
      msg.obj = message;
      mMaster.sendMessage(msg);
   }


   /**
    * Process incoming WebSockets data (after handshake).
    */
   private boolean processData() throws Exception {

      // outside frame?
      if (mFrameHeader == null) {

         // need at least 2 bytes from WS frame header to start processing
         if (mBuffer.position() >= 2) {

            byte b0 = mBuffer.get(0);
            boolean fin = (b0 & 0x80) != 0;
            int rsv = (b0 & 0x70) >> 4;
            int opcode = b0 & 0x0f;

            byte b1 = mBuffer.get(1);
            boolean masked = (b1 & 0x80) != 0;
            int payload_len1 = b1 & 0x7f;

            // now check protocol compliance

            if (rsv != 0) {
               throw new WebSocketException("RSV != 0 and no extension negotiated");
            }

            if (masked) {
               // currently, we don't allow this. need to see whats the final spec.
               throw new WebSocketException("masked server frame");
            }

            if (opcode > 7) {
               // control frame
               if (!fin) {
                  throw new WebSocketException("fragmented control frame");
               }
               if (payload_len1 > 125) {
                  throw new WebSocketException("control frame with payload length > 125 octets");
               }
               if (opcode != 8 && opcode != 9 && opcode != 10) {
                  throw new WebSocketException("control frame using reserved opcode " + opcode);
               }
               if (opcode == 8 && payload_len1 == 1) {
                  throw new WebSocketException("received close control frame with payload len 1");
               }
            } else {
               // message frame
               if (opcode != 0 && opcode != 1 && opcode != 2) {
                  throw new WebSocketException("data frame using reserved opcode " + opcode);
               }
               if (!mInsideMessage && opcode == 0) {
                  throw new WebSocketException("received continuation data frame outside fragmented message");
               }
               if (mInsideMessage && opcode != 0) {
                  throw new WebSocketException("received non-continuation data frame while inside fragmented message");
               }
            }

            int mask_len = masked ? 4 : 0;
            int header_len = 0;

            if (payload_len1 < 126) {
               header_len = 2 + mask_len;
            } else if (payload_len1 == 126) {
               header_len = 2 + 2 + mask_len;
            } else if (payload_len1 == 127) {
               header_len = 2 + 8 + mask_len;
            } else {
               // should not arrive here
               throw new Exception("logic error");
            }

            // continue when complete frame header is available
            if (mBuffer.position() >= header_len) {

               // determine frame payload length
               int i = 2;
               long payload_len = 0;
               if (payload_len1 == 126) {
                  payload_len = ((0xff & mBuffer.get(i)) << 8) | (0xff & mBuffer.get(i+1));
                  if (payload_len < 126) {
                     throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
                  }
                  i += 2;
               } else if (payload_len1 == 127) {
                  if ((0x80 & mBuffer.get(i+0)) != 0) {
                     throw new WebSocketException("invalid data frame length (> 2^63)");
                  }
                  payload_len = ((0xff & mBuffer.get(i+0)) << 56) |
                                ((0xff & mBuffer.get(i+1)) << 48) |
                                ((0xff & mBuffer.get(i+2)) << 40) |
                                ((0xff & mBuffer.get(i+3)) << 32) |
                                ((0xff & mBuffer.get(i+4)) << 24) |
                                ((0xff & mBuffer.get(i+5)) << 16) |
                                ((0xff & mBuffer.get(i+6)) <<  8) |
                                ((0xff & mBuffer.get(i+7))      );
                  if (payload_len < 65536) {
                     throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
                  }
                  i += 8;
               } else {
                  payload_len = payload_len1;
               }

               // immediately bail out on frame too large
               if (payload_len > mOptions.getMaxFramePayloadSize()) {
                  throw new WebSocketException("frame payload too large");
               }

               // save frame header metadata
               mFrameHeader = new FrameHeader();
               mFrameHeader.mOpcode = opcode;
               mFrameHeader.mFin = fin;
               mFrameHeader.mReserved = rsv;
               mFrameHeader.mPayloadLen = (int) payload_len;
               mFrameHeader.mHeaderLen = header_len;
               mFrameHeader.mTotalLen = mFrameHeader.mHeaderLen + mFrameHeader.mPayloadLen;
               if (masked) {
                  mFrameHeader.mMask = new byte[4];
                  for (int j = 0; j < 4; ++j) {
                     mFrameHeader.mMask[i] = (byte) (0xff & mBuffer.get(i + j));
                  }
                  i += 4;
               } else {
                  mFrameHeader.mMask = null;
               }

               // continue processing when payload empty or completely buffered
               return mFrameHeader.mPayloadLen == 0 || mBuffer.position() >= mFrameHeader.mTotalLen;

            } else {

               // need more data
               return false;
            }
         } else {

            // need more data
            return false;
         }

      } else {

         /// \todo refactor this for streaming processing, incl. fail fast on invalid UTF-8 within frame already

         // within frame

         // see if we buffered complete frame
         if (mBuffer.position() >= mFrameHeader.mTotalLen) {

            // cut out frame payload
            byte[] framePayload = null;
            int oldPosition = mBuffer.position();
            if (mFrameHeader.mPayloadLen > 0) {
               framePayload = new byte[mFrameHeader.mPayloadLen];
               mBuffer.position(mFrameHeader.mHeaderLen);
               mBuffer.get(framePayload, 0, (int) mFrameHeader.mPayloadLen);
            }
            mBuffer.position(mFrameHeader.mTotalLen);
            mBuffer.limit(oldPosition);
            mBuffer.compact();

            if (mFrameHeader.mOpcode > 7) {
               // control frame

               if (mFrameHeader.mOpcode == 8) {

                  int code = 1005; // CLOSE_STATUS_CODE_NULL : no status code received
                  String reason = null;

                  if (mFrameHeader.mPayloadLen >= 2) {

                     // parse and check close code
                     code = (framePayload[0] & 0xff) * 256 + (framePayload[1] & 0xff);
                     if (code < 1000
                           || (code >= 1000 && code <= 2999 &&
                               code != 1000 && code != 1001 && code != 1002 && code != 1003 && code != 1007 && code != 1008 && code != 1009 && code != 1010 && code != 1011)
                           || code >= 5000) {

                        throw new WebSocketException("invalid close code " + code);
                     }

                     // parse and check close reason
                     if (mFrameHeader.mPayloadLen > 2) {

                        byte[] ra = new byte[mFrameHeader.mPayloadLen - 2];
                        System.arraycopy(framePayload, 2, ra, 0, mFrameHeader.mPayloadLen - 2);

                        Utf8Validator val = new Utf8Validator();
                        val.validate(ra);
                        if (!val.isValid()) {
                           throw new WebSocketException("invalid close reasons (not UTF-8)");
                        } else {
                           reason = new String(ra, "UTF-8");
                        }
                     }
                  }
                  onClose(code, reason);

               } else if (mFrameHeader.mOpcode == 9) {
                  // dispatch WS ping
                  onPing(framePayload);

               } else if (mFrameHeader.mOpcode == 10) {
                  // dispatch WS pong
                  onPong(framePayload);

               } else {

                  // should not arrive here (handled before)
                  throw new Exception("logic error");
               }

            } else {
               // message frame

               if (!mInsideMessage) {
                  // new message started
                  mInsideMessage = true;
                  mMessageOpcode = mFrameHeader.mOpcode;
                  if (mMessageOpcode == 1 && mOptions.getValidateIncomingUtf8()) {
                     mUtf8Validator.reset();
                  }
               }

               if (framePayload != null) {

                  // immediately bail out on message too large
                  if (mMessagePayload.size() + framePayload.length > mOptions.getMaxMessagePayloadSize()) {
                     throw new WebSocketException("message payload too large");
                  }

                  // validate incoming UTF-8
                  if (mMessageOpcode == 1 && mOptions.getValidateIncomingUtf8() && !mUtf8Validator.validate(framePayload)) {
                     throw new WebSocketException("invalid UTF-8 in text message payload");
                  }

                  // buffer frame payload for message
                  mMessagePayload.write(framePayload);
               }

               // on final frame ..
               if (mFrameHeader.mFin) {

                  if (mMessageOpcode == 1) {

                     // verify that UTF-8 ends on codepoint
                     if (mOptions.getValidateIncomingUtf8() && !mUtf8Validator.isValid()) {
                        throw new WebSocketException("UTF-8 text message payload ended within Unicode code point");
                     }

                     // deliver text message
                     if (mOptions.getReceiveTextMessagesRaw()) {

                        // dispatch WS text message as raw (but validated) UTF-8
                        onRawTextMessage(mMessagePayload.toByteArray());

                     } else {

                        // dispatch WS text message as Java String (previously already validated)
                        String s = new String(mMessagePayload.toByteArray(), "UTF-8");
                        onTextMessage(s);
                     }

                  } else if (mMessageOpcode == 2) {

                     // dispatch WS binary message
                     onBinaryMessage(mMessagePayload.toByteArray());

                  } else {

                     // should not arrive here (handled before)
                     throw new Exception("logic error");
                  }

                  // ok, message completed - reset all
                  mInsideMessage = false;
                  mMessagePayload.reset();
               }
            }

            // reset frame
            mFrameHeader = null;

            // reprocess if more data left
            return mBuffer.position() > 0;

         } else {

            // need more data
            return false;
         }
      }
   }


   /**
    * WebSockets handshake reply from server received, default notifies master.
    * 
    * @param success	Success handshake flag
    */
   protected void onHandshake(boolean success) {

      notify(new WebSocketMessage.ServerHandshake(success));
   }


   /**
    * WebSockets close received, default notifies master.
    */
   protected void onClose(int code, String reason) {

      notify(new WebSocketMessage.Close(code, reason));
   }


   /**
    * WebSockets ping received, default notifies master.
    *
    * @param payload    Ping payload or null.
    */
   protected void onPing(byte[] payload) {

      notify(new WebSocketMessage.Ping(payload));
   }


   /**
    * WebSockets pong received, default notifies master.
    *
    * @param payload    Pong payload or null.
    */
   protected void onPong(byte[] payload) {

      notify(new WebSocketMessage.Pong(payload));
   }


   /**
    * WebSockets text message received, default notifies master.
    * This will only be called when the option receiveTextMessagesRaw
    * HAS NOT been set.
    *
    * @param payload    Text message payload as Java String decoded
    *                   from raw UTF-8 payload or null (empty payload).
    */
   protected void onTextMessage(String payload) {

      notify(new WebSocketMessage.TextMessage(payload));
   }


   /**
    * WebSockets text message received, default notifies master.
    * This will only be called when the option receiveTextMessagesRaw
    * HAS been set.
    *
    * @param payload    Text message payload as raw UTF-8 octets or
    *                   null (empty payload).
    */
   protected void onRawTextMessage(byte[] payload) {

      notify(new WebSocketMessage.RawTextMessage(payload));
   }


   /**
    * WebSockets binary message received, default notifies master.
    *
    * @param payload    Binary message payload or null (empty payload).
    */
   protected void onBinaryMessage(byte[] payload) {

      notify(new WebSocketMessage.BinaryMessage(payload));
   }


   /**
    * Process WebSockets handshake received from server.
    */
   private boolean processHandshake() throws UnsupportedEncodingException {

      boolean res = false;
      for (int pos = mBuffer.position() - 4; pos >= 0; --pos) {
         if (mBuffer.get(pos+0) == 0x0d &&
             mBuffer.get(pos+1) == 0x0a &&
             mBuffer.get(pos+2) == 0x0d &&
             mBuffer.get(pos+3) == 0x0a) {

            /// \todo process & verify handshake from server
            /// \todo forward subprotocol, if any

            int oldPosition = mBuffer.position();
            
            // Check HTTP status code
            boolean serverError = false;
            if (mBuffer.get(0) == 'H' &&
            	mBuffer.get(1) == 'T' &&
            	mBuffer.get(2) == 'T' &&
            	mBuffer.get(3) == 'P') {
            	
            	Pair<Integer, String> status = parseHttpStatus();
            	if (status.first >= 300) {
            		// Invalid status code for success connection
            		notify(new WebSocketMessage.ServerError(status.first, status.second));
            		serverError = true;
            	}
            }
            
            mBuffer.position(pos + 4);
            mBuffer.limit(oldPosition);
            mBuffer.compact();

            if (!serverError) {
            	// process further when data after HTTP headers left in buffer
                res = mBuffer.position() > 0;

                mState = STATE_OPEN;
            } else {
            	res = true;
            	mState = STATE_CLOSED;
            	mStopped = true;
            }
            
            onHandshake(!serverError);
            break;
         }
      }
      return res;
   }
   
   @SuppressWarnings("unused")
   private Map<String, String> parseHttpHeaders(byte[] buffer) throws UnsupportedEncodingException {
	   // TODO: use utf-8 validator?
	   String s = new String(buffer, "UTF-8");
	   Map<String, String> headers = new HashMap<String, String>();
	   
	   String[] lines = s.split("\r\n");
	   for (String line : lines) {
		   if (line.length() > 0) {
			   String[] h = line.split(": ");
			   if (h.length == 2) {
				   headers.put(h[0], h[1]);
				   Log.w(TAG, String.format("'%s'='%s'", h[0], h[1]));
			   }
		   }
	   }
	   
	   return headers;
   }
   
   private Pair<Integer, String> parseHttpStatus() throws UnsupportedEncodingException {
	   int beg, end;
		// Find first space
		for (beg = 4; beg < mBuffer.position(); ++beg) {
			if (mBuffer.get(beg) == ' ') break;
		}
		// Find second space
		for (end = beg + 1; end < mBuffer.position(); ++end) {
			if (mBuffer.get(end) == ' ') break;
		}
		// Parse status code between them
		++beg;
		int statusCode = 0;
		for (int i = 0; beg + i < end; ++i) {
			int digit = (mBuffer.get(beg + i) - 0x30);
			statusCode *= 10;
			statusCode += digit;
		}
		// Find end of line to extract error message
		++end;
		int eol;
		for (eol = end; eol < mBuffer.position(); ++eol) {
			if (mBuffer.get(eol) == 0x0d) break;
		}
		int statusMessageLength = eol - end;
		byte[] statusBuf = new byte[statusMessageLength];
		mBuffer.position(end);
		mBuffer.get(statusBuf, 0, statusMessageLength);
		String statusMessage = new String(statusBuf, "UTF-8");
		if (DEBUG) Log.w(TAG, String.format("Status: %d (%s)", statusCode, statusMessage));
		return new Pair<Integer, String>(statusCode, statusMessage);
   }


   /**
    * Consume data buffered in mFrameBuffer.
    */
   private boolean consumeData() throws Exception {

      if (mState == STATE_OPEN || mState == STATE_CLOSING) {

         return processData();

      } else if (mState == STATE_CONNECTING) {

         return processHandshake();

      } else if (mState == STATE_CLOSED) {

         return false;

      } else {
         // should not arrive here
         return false;
      }

   }

   private void runDelegatedTasks(SSLEngineResult result) throws Exception {
      
      if (DEBUG) Log.d(TAG, "SSL result: " + result.toString());

      if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
         Runnable runnable;
         while ((runnable = mSSLEngine.getDelegatedTask()) != null) {
            if (DEBUG) Log.d(TAG, "running SSL delegated task");
            runnable.run();
         }
         HandshakeStatus hsStatus = mSSLEngine.getHandshakeStatus();
         if (hsStatus == HandshakeStatus.NEED_TASK) {
            throw new Exception("handshake shouldn't need additional tasks");
         }
      }
   }
   
   /**
    * Run the background reader thread loop.
    */
   @Override
   public void run() {

      if (DEBUG) Log.d(TAG, "running");

      try {

         mBuffer.clear();
         do {
            int len = 0;
            
            // blocking read on socket
            if (mSSLEngine != null) {
               mBufferEnc.clear();
               len = mSocket.read(mBufferEnc);
               if (DEBUG) Log.d(TAG, "READ (WSS): " + len + " - " + mBufferEnc.remaining() + " - " + mSocket.isBlocking());
            } else {
               len = mSocket.read(mBuffer);               
               if (DEBUG) Log.d(TAG, "READ (WS): " + len);
            }
            
            if (len > 0) {
               // decrypt data
               if (mSSLEngine != null) {
                  mBufferEnc.flip();
                  //mBuffer.clear();
                  SSLEngineResult res;
                  do {
                     if (DEBUG) Log.d(TAG, "before UNWRAP: " + mBuffer.position() + " - " + mBuffer.limit() + " - " + mBuffer.remaining() + " - " + mBuffer.mark() + " - " + mBuffer.capacity());
                     res = mSSLEngine.unwrap(mBufferEnc, mBuffer);
                     runDelegatedTasks(res);
                     if (DEBUG) Log.d(TAG, "after  UNWRAP: " + mBuffer.position() + " - " + mBuffer.limit() + " - " + mBuffer.remaining() + " - " + mBuffer.mark() + " - " + mBuffer.capacity());
                  //} while (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP);
                  } while (mBufferEnc.hasRemaining());
                  //mBuffer.flip();
               }
               
               // process buffered data
               while (consumeData()) {
                  // consume further until all processed
               }
            } else if (len < 0) {
               // shit happened
               if (DEBUG) Log.d(TAG, "run() : ConnectionLost");
               notify(new WebSocketMessage.ConnectionLost());
               mStopped = true;
            } else {
               // len == 0: nothing to do, but continue
            }
            
         } while (!mStopped);

      } catch (WebSocketException e) {

         if (DEBUG) Log.d(TAG, "run() : WebSocketException (" + e.toString() + ")");

         // wrap the exception and notify master
         notify(new WebSocketMessage.ProtocolViolation(e));

      } catch (SocketException e) {
    	  
    	  if (DEBUG) Log.d(TAG, "run() : SocketException (" + e.toString() + ")");
    	  
    	  // wrap the exception and notify master
    	  notify(new WebSocketMessage.ConnectionLost());;
    	  
      } catch (Exception e) {

         if (DEBUG) Log.d(TAG, "run() : Exception (" + e.toString() + ")");

         // wrap the exception and notify master
         notify(new WebSocketMessage.Error(e));

      } finally {

         mStopped = true;
      }

      if (DEBUG) Log.d(TAG, "ended");
   }
}
