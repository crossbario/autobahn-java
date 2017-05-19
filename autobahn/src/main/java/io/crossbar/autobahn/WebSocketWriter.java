/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package io.crossbar.autobahn;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import org.apache.http.NameValuePair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

/**
 * WebSocket writer, the sending leg of a WebSockets connection.
 * This is run on it's background thread with it's own message loop.
 * The only method that needs to be called (from foreground thread) is forward(),
 * which is used to forward a WebSockets message to this object (running on
 * background thread) so that it can be formatted and sent out on the
 * underlying TCP socket.
 */
public class WebSocketWriter extends Handler {

    private static final boolean DEBUG = true;
    private static final String TAG = WebSocketWriter.class.getName();
    private final static String CRLF = "\r\n";

    /// Random number generator for handshake key and frame mask generation.
    private final Random mRng = new Random();

    /// Connection master.
    private final Handler mMaster;

    /// Message looper this object is running on.
    private final Looper mLooper;

    /// WebSockets options.
    private final WebSocketOptions mOptions;

    /// The send buffer that holds data to send on socket.
    private BufferedOutputStream mBufferedOutputStream;

    /// The tcp socket
    private Socket mSocket;

    /// Is Active.
    private boolean mActive;


    /**
     * Create new WebSockets background writer.
     *
     * @param looper  The message looper of the background thread on which
     *                this object is running.
     * @param master  The message handler of master (foreground thread).
     * @param socket  The socket channel created on foreground thread.
     * @param options WebSockets connection options.
     */
    public WebSocketWriter(Looper looper, Handler master, Socket socket, WebSocketOptions options) throws IOException {

        super(looper);

        mLooper = looper;
        mMaster = master;
        mOptions = options;
        mSocket = socket;
        mBufferedOutputStream = new BufferedOutputStream(socket.getOutputStream(), options.getMaxFramePayloadSize() + 14);
        mActive = true;

        if (DEBUG) Log.d(TAG, "created");
    }

    private void write(String stringToWrite) {
        try {
            mBufferedOutputStream.write(stringToWrite.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(byte byteToWrite) {
        try {
            mBufferedOutputStream.write(byteToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(byte[] bytesToWrite) {
        try {
            mBufferedOutputStream.write(bytesToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this from the foreground (UI) thread to make the writer
     * (running on background thread) send a WebSocket message on the
     * underlying TCP.
     *
     * @param message Message to send to WebSockets writer. An instance of the message
     *                classes inside WebSocketMessage or another type which then needs
     *                to be handled within processAppMessage() (in a class derived from
     *                this class).
     */
    public void forward(Object message) {
        // We have already quit, we are no longer sending messages.
        if (!mActive) {
            if (DEBUG) Log.d(TAG, "We have already quit, not processing further messages");
            return;
        }
        Message msg = obtainMessage();
        msg.obj = message;
        sendMessage(msg);
    }


    /**
     * Notify the master (foreground thread).
     *
     * @param message Message to send to master.
     */
    private void notify(Object message) {

        Message msg = mMaster.obtainMessage();
        msg.obj = message;
        mMaster.sendMessage(msg);
    }


    /**
     * Create new key for WebSockets handshake.
     *
     * @return WebSockets handshake key (Base64 encoded).
     */
    private String newHandshakeKey() {
        final byte[] ba = new byte[16];
        mRng.nextBytes(ba);
        return Base64.encodeToString(ba, Base64.NO_WRAP);
    }


    /**
     * Create new (random) frame mask.
     *
     * @return Frame mask (4 octets).
     */
    private byte[] newFrameMask() {
        final byte[] ba = new byte[4];
        mRng.nextBytes(ba);
        return ba;
    }


    /**
     * Send WebSocket client handshake.
     */
    private void sendClientHandshake(WebSocketMessage.ClientHandshake message) throws IOException {

        // write HTTP header with handshake
        String path;
        if (message.mQuery != null) {
            path = message.mPath + "?" + message.mQuery;
        } else {
            path = message.mPath;
        }
        write("GET " + path + " HTTP/1.1");
        write(CRLF);
        write("Host: " + message.mHost);
        write(CRLF);
        write("Upgrade: WebSocket");
        write(CRLF);
        write("Connection: Upgrade");
        write(CRLF);

        write("Sec-WebSocket-Key: " + newHandshakeKey());
        write(CRLF);

        if (message.mOrigin != null && !message.mOrigin.equals("")) {
            write("Origin: " + message.mOrigin);
            write(CRLF);
        }

        if (message.mSubprotocols != null && message.mSubprotocols.length > 0) {
            write("Sec-WebSocket-Protocol: ");
            for (int i = 0; i < message.mSubprotocols.length; ++i) {
                write(message.mSubprotocols[i]);
                if (i != message.mSubprotocols.length - 1) {
                    write(", ");
                }
            }
            write(CRLF);
        }

        write("Sec-WebSocket-Version: 13");
        write(CRLF);

        // Header injection
        if (message.mHeaderList != null) {
            for (NameValuePair pair : message.mHeaderList) {
                write(pair.getName() + ":" + pair.getValue());
                write(CRLF);
            }
        }
        write(CRLF);
    }


    /**
     * Send WebSockets close.
     */
    private void sendClose(WebSocketMessage.Close message) throws IOException, WebSocketException {

        if (message.mCode > 0) {

            byte[] payload;

            if (message.mReason != null && !message.mReason.equals("")) {
                byte[] pReason = message.mReason.getBytes("UTF-8");
                payload = new byte[2 + pReason.length];
                for (int i = 0; i < pReason.length; ++i) {
                    payload[i + 2] = pReason[i];
                }
            } else {
                payload = new byte[2];
            }

            if (payload != null && payload.length > 125) {
                throw new WebSocketException("close payload exceeds 125 octets");
            }

            payload[0] = (byte) ((message.mCode >> 8) & 0xff);
            payload[1] = (byte) (message.mCode & 0xff);

            sendFrame(8, true, payload);

        } else {

            sendFrame(8, true, null);
        }
    }


    /**
     * Send WebSockets ping.
     */
    private void sendPing(WebSocketMessage.Ping message) throws IOException, WebSocketException {
        if (message.mPayload != null && message.mPayload.length > 125) {
            throw new WebSocketException("ping payload exceeds 125 octets");
        }
        sendFrame(9, true, message.mPayload);
    }


    /**
     * Send WebSockets pong. Normally, unsolicited Pongs are not used,
     * but Pongs are only send in response to a Ping from the peer.
     */
    private void sendPong(WebSocketMessage.Pong message) throws IOException, WebSocketException {
        if (message.mPayload != null && message.mPayload.length > 125) {
            throw new WebSocketException("pong payload exceeds 125 octets");
        }
        sendFrame(10, true, message.mPayload);
    }


    /**
     * Send WebSockets binary message.
     */
    private void sendBinaryMessage(WebSocketMessage.BinaryMessage message) throws IOException, WebSocketException {
        if (message.mPayload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        sendFrame(2, true, message.mPayload);
    }


    /**
     * Send WebSockets text message.
     */
    private void sendTextMessage(WebSocketMessage.TextMessage message) throws IOException, WebSocketException {
        byte[] payload = message.mPayload.getBytes("UTF-8");
        if (payload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        sendFrame(1, true, payload);
    }


    /**
     * Send WebSockets binary message.
     */
    private void sendRawTextMessage(WebSocketMessage.RawTextMessage message) throws IOException, WebSocketException {
        if (message.mPayload.length > mOptions.getMaxMessagePayloadSize()) {
            throw new WebSocketException("message payload exceeds payload limit");
        }
        sendFrame(1, true, message.mPayload);
    }


    /**
     * Sends a WebSockets frame. Only need to use this method in derived classes which implement
     * more message types in processAppMessage(). You need to know what you are doing!
     *
     * @param opcode  The WebSocket frame opcode.
     * @param fin     FIN flag for WebSocket frame.
     * @param payload Frame payload or null.
     */
    protected void sendFrame(int opcode, boolean fin, byte[] payload) throws IOException {
        if (payload != null) {
            sendFrame(opcode, fin, payload, 0, payload.length);
        } else {
            sendFrame(opcode, fin, null, 0, 0);
        }
    }


    /**
     * Sends a WebSockets frame. Only need to use this method in derived classes which implement
     * more message types in processAppMessage(). You need to know what you are doing!
     *
     * @param opcode  The WebSocket frame opcode.
     * @param fin     FIN flag for WebSocket frame.
     * @param payload Frame payload or null.
     * @param offset  Offset within payload of the chunk to send.
     * @param length  Length of the chunk within payload to send.
     */
    protected void sendFrame(int opcode, boolean fin, byte[] payload, int offset, int length) throws IOException {

        // first octet
        byte b0 = 0;
        if (fin) {
            b0 |= (byte) (1 << 7);
        }
        b0 |= (byte) opcode;
        write(b0);

        // second octet
        byte b1 = 0;
        if (mOptions.getMaskClientFrames()) {
            b1 = (byte) (1 << 7);
        }

        long len = length;

        // extended payload length
        if (len <= 125) {
            b1 |= (byte) len;
            write(b1);
        } else if (len <= 0xffff) {
            b1 |= (byte) (126 & 0xff);
            write(b1);
            write(new byte[]{(byte) ((len >> 8) & 0xff),
                    (byte) (len & 0xff)});
        } else {
            b1 |= (byte) (127 & 0xff);
            write(b1);
            write(new byte[]{(byte) ((len >> 56) & 0xff),
                    (byte) ((len >> 48) & 0xff),
                    (byte) ((len >> 40) & 0xff),
                    (byte) ((len >> 32) & 0xff),
                    (byte) ((len >> 24) & 0xff),
                    (byte) ((len >> 16) & 0xff),
                    (byte) ((len >> 8) & 0xff),
                    (byte) (len & 0xff)});
        }

        byte mask[] = null;
        if (mOptions.getMaskClientFrames()) {
            // a mask is always needed, even without payload
            mask = newFrameMask();
            write(mask[0]);
            write(mask[1]);
            write(mask[2]);
            write(mask[3]);
        }

        if (len > 0) {
            if (mOptions.getMaskClientFrames()) {
                /// \todo optimize masking
                /// \todo masking within buffer of output stream
                for (int i = 0; i < len; ++i) {
                    payload[i + offset] ^= mask[i % 4];
                }
            }
            mBufferedOutputStream.write(payload, offset, length);
        }
    }


    /**
     * Process message received from foreground thread. This is called from
     * the message looper set up for the background thread running this writer.
     *
     * @param msg Message from thread message queue.
     */
    @Override
    public void handleMessage(Message msg) {

        try {

            // process message from master
            processMessage(msg.obj);

            // send out buffered data

            if (mActive && mSocket.isConnected() && !mSocket.isClosed()) {
                mBufferedOutputStream.flush();
            }

            // Check if the message that we sent was a close frame and was a reply
            // to a closing handshake, if so, then notify master to close the socket.
            if (msg.obj instanceof WebSocketMessage.Close) {
                WebSocketMessage.Close closeMessage = (WebSocketMessage.Close) msg.obj;
                if (closeMessage.mIsReply) {
                    notify(new WebSocketMessage.Close(closeMessage.mCode, closeMessage.mReason, true));
                }
            }


        } catch (SocketException e) {

            if (DEBUG) Log.d(TAG, "run() : SocketException (" + e.toString() + ")");

            // wrap the exception and notify master
            notify(new WebSocketMessage.ConnectionLost());
        } catch (Exception e) {

            if (DEBUG) e.printStackTrace();

            // wrap the exception and notify master
            notify(new WebSocketMessage.Error(e));
        }
    }


    /**
     * Process WebSockets or control message from master. Normally,
     * there should be no reason to override this. If you do, you
     * need to know what you are doing.
     *
     * @param msg An instance of the message types within WebSocketMessage
     *            or a message that is handled in processAppMessage().
     */
    protected void processMessage(Object msg) throws IOException, WebSocketException {

        if (msg instanceof WebSocketMessage.TextMessage) {

            sendTextMessage((WebSocketMessage.TextMessage) msg);

        } else if (msg instanceof WebSocketMessage.RawTextMessage) {

            sendRawTextMessage((WebSocketMessage.RawTextMessage) msg);

        } else if (msg instanceof WebSocketMessage.BinaryMessage) {

            sendBinaryMessage((WebSocketMessage.BinaryMessage) msg);

        } else if (msg instanceof WebSocketMessage.Ping) {

            sendPing((WebSocketMessage.Ping) msg);

        } else if (msg instanceof WebSocketMessage.Pong) {

            sendPong((WebSocketMessage.Pong) msg);

        } else if (msg instanceof WebSocketMessage.Close) {

            sendClose((WebSocketMessage.Close) msg);

        } else if (msg instanceof WebSocketMessage.ClientHandshake) {
            sendClientHandshake((WebSocketMessage.ClientHandshake) msg);

        } else if (msg instanceof WebSocketMessage.Quit) {

            mLooper.quit();
            mActive = false;

            if (DEBUG) Log.d(TAG, "ended");

        } else {

            // call hook which may be overridden in derived class to process
            // messages we don't understand in this class
            processAppMessage(msg);
        }
    }


    /**
     * Process message other than plain WebSockets or control message.
     * This is intended to be overridden in derived classes.
     *
     * @param msg Message from foreground thread to process.
     */
    protected void processAppMessage(Object msg) throws WebSocketException, IOException {

        throw new WebSocketException("unknown message received by WebSocketWriter");
    }
}
