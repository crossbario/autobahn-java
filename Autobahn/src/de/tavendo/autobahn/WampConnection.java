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

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.type.TypeReference;

import android.os.HandlerThread;
import android.util.Log;

public class WampConnection extends WebSocketConnection implements Wamp {

   private static final boolean DEBUG = true;
   private static final String TAG = WampConnection.class.getName();


   /// The message handler of the background writer.
   protected WampWriter mWriterHandler;

   /// Prefix map for outgoing messages.
   private final PrefixMap mOutgoingPrefixes = new PrefixMap();

   /// RNG for IDs.
   private final Random mRng = new Random();

   /// Set of chars to be used for IDs.
   private static final char[] mBase64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
         .toCharArray();

   /**
    * RPC metadata.
    */
   public static class CallMeta {

      CallMeta(CallHandler handler, Class<?> resultClass) {
         this.mResultHandler = handler;
         this.mResultClass = resultClass;
         this.mResultTypeRef = null;
      }

      CallMeta(CallHandler handler, TypeReference<?> resultTypeReference) {
         this.mResultHandler = handler;
         this.mResultClass = null;
         this.mResultTypeRef = resultTypeReference;
      }

      /// Call handler to be fired on.
      public CallHandler mResultHandler;

      /// Desired call result type or null.
      public Class<?> mResultClass;

      /// Desired call result type or null.
      public TypeReference<?> mResultTypeRef;
   }

   /// Metadata about issued, but not yet returned RPCs.
   private final ConcurrentHashMap<String, CallMeta> mCalls = new ConcurrentHashMap<String, CallMeta>();

   /**
    * Event subscription metadata.
    */
   public static class SubMeta {

      SubMeta(EventHandler handler, Class<?> resultClass) {
         this.mEventHandler = handler;
         this.mEventClass = resultClass;
         this.mEventTypeRef = null;
      }

      SubMeta(EventHandler handler, TypeReference<?> resultTypeReference) {
         this.mEventHandler = handler;
         this.mEventClass = null;
         this.mEventTypeRef = resultTypeReference;
      }

      /// Event handler to be fired on.
      public EventHandler mEventHandler;

      /// Desired event type or null.
      public Class<?> mEventClass;

      /// Desired event type or null.
      public TypeReference<?> mEventTypeRef;
   }

   /// Metadata about active event subscriptions.
   private final ConcurrentHashMap<String, SubMeta> mSubs = new ConcurrentHashMap<String, SubMeta>();

   /// The session handler provided to connect().
   private Wamp.ConnectionHandler mSessionHandler;


   /**
    * Create the connection transmitting leg writer.
    */
   protected void createWriter() {

      mWriterThread = new HandlerThread("AutobahnWriter");
      mWriterThread.start();
      mWriter = new WampWriter(mWriterThread.getLooper(), mMasterHandler, mTransportChannel, mOptions);

      if (DEBUG) Log.d(TAG, "writer created and started");
   }


   /**
    * Create the connection receiving leg reader.
    */
   protected void createReader() {
      mReader = new WampReader(mCalls, mSubs, mMasterHandler, mTransportChannel, mOptions, "AutobahnReader");
      mReader.start();

      if (DEBUG) Log.d(TAG, "reader created and started");
   }


   /**
    * Create new random ID. This is used, i.e. for use in RPC calls to correlate
    * call message with result message.
    *
    * @param len    Length of ID.
    * @return       New random ID of given length.
    */
   private String newId(int len) {
      char[] buffer = new char[len];
      for (int i = 0; i < len; i++) {
         buffer[i] = mBase64Chars[mRng.nextInt(mBase64Chars.length)];
      }
      return new String(buffer);
   }


   /**
    * Create new random ID of default length.
    *
    * @return    New random ID of default length.
    */
   private String newId() {
      return newId(8);
   }


   public void connect(String wsUri, Wamp.ConnectionHandler sessionHandler) {

      WampOptions options = new WampOptions();
      options.setReceiveTextMessagesRaw(true);
      options.setMaxMessagePayloadSize(64*1024);
      options.setMaxFramePayloadSize(64*1024);
      options.setTcpNoDelay(true);

      connect(wsUri, sessionHandler, options, null);
   }


   /**
    * Connect to server.
    *
    * @param wsUri            WebSockets server URI.
    * @param sessionHandler   The session handler to fire callbacks on.
    * @param headers		   The headers for connection
    */
   public void connect(String wsUri, Wamp.ConnectionHandler sessionHandler, WampOptions options, List<BasicNameValuePair> headers) {

      mSessionHandler = sessionHandler;

      mCalls.clear();
      mSubs.clear();
      mOutgoingPrefixes.clear();

      try {
         connect(wsUri, new String[] {"wamp"}, new WebSocketConnectionHandler() {

            @Override
            public void onOpen() {
               if (mSessionHandler != null) {
                  mSessionHandler.onOpen();
               } else {
                  if (DEBUG) Log.d(TAG, "could not call onOpen() .. handler already NULL");
               }
            }

            @Override
            public void onClose(int code, String reason) {
               if (mSessionHandler != null) {
                  mSessionHandler.onClose(code, reason);
               } else {
                  if (DEBUG) Log.d(TAG, "could not call onClose() .. handler already NULL");
               }
            }

         }, options, headers);

      } catch (WebSocketException e) {

         if (mSessionHandler != null) {
            mSessionHandler.onClose(WebSocketConnectionHandler.CLOSE_CANNOT_CONNECT, "cannot connect (" + e.toString() + ")");
         } else {
            if (DEBUG) Log.d(TAG, "could not call onClose() .. handler already NULL");
         }
      }

   }
	
   public void connect(String wsUri, Wamp.ConnectionHandler sessionHandler, List<BasicNameValuePair> headers) {

	   WampOptions options = new WampOptions();
	   options.setReceiveTextMessagesRaw(true);
	   options.setMaxMessagePayloadSize(64*1024);
	   options.setMaxFramePayloadSize(64*1024);
	   options.setTcpNoDelay(true);

	   connect(wsUri, sessionHandler, options, headers);
   }

   public void connect(String wsUri, Wamp.ConnectionHandler sessionHandler, WampOptions options) {	

	   connect(wsUri, sessionHandler, options, null);
   }


   /**
    * Process WAMP messages coming from the background reader.
    */
   protected void processAppMessage(Object message) {

      if (message instanceof WampMessage.CallResult) {

         WampMessage.CallResult callresult = (WampMessage.CallResult) message;

         if (mCalls.containsKey(callresult.mCallId)) {
            CallMeta meta = mCalls.get(callresult.mCallId);
            if (meta.mResultHandler != null) {
               meta.mResultHandler.onResult(callresult.mResult);
            }
            mCalls.remove(callresult.mCallId);
         }

      } else if (message instanceof WampMessage.CallError) {

         WampMessage.CallError callerror = (WampMessage.CallError) message;

         if (mCalls.containsKey(callerror.mCallId)) {
            CallMeta meta = mCalls.get(callerror.mCallId);
            if (meta.mResultHandler != null) {
               meta.mResultHandler.onError(callerror.mErrorUri, callerror.mErrorDesc);
            }
            mCalls.remove(callerror.mCallId);
         }
      } else if (message instanceof WampMessage.Event) {

         WampMessage.Event event = (WampMessage.Event) message;

         if (mSubs.containsKey(event.mTopicUri)) {
            SubMeta meta = mSubs.get(event.mTopicUri);
            if (meta != null && meta.mEventHandler != null) {
               meta.mEventHandler.onEvent(event.mTopicUri, event.mEvent);
            }
         }
      } else if (message instanceof WampMessage.Welcome) {

         WampMessage.Welcome welcome = (WampMessage.Welcome) message;

         // FIXME: safe session ID / fire session opened hook
         if (DEBUG) Log.d(TAG, "WAMP session " + welcome.mSessionId + " established (protocol version " + welcome.mProtocolVersion + ", server " + welcome.mServerIdent + ")");

      } else {

         if (DEBUG) Log.d(TAG, "unknown WAMP message in AutobahnConnection.processAppMessage");
      }
   }


   /**
    * Issue a remote procedure call (RPC).
    *
    * @param procUri       URI or CURIE of procedure to call.
    * @param resultMeta    Call result metadata.
    * @param arguments     Call arguments.
    */
   private void call(String procUri, CallMeta resultMeta, Object... arguments) {

      WampMessage.Call call = new WampMessage.Call(newId(), procUri, arguments.length);
      for (int i = 0; i < arguments.length; ++i) {
         call.mArgs[i] = arguments[i];
      }
      mCalls.put(call.mCallId, resultMeta);
      mWriter.forward(call);
   }


   /**
    * Issue a remote procedure call (RPC). This version should be used with
    * primitive Java types and simple composite (class) types.
    *
    * @param procUri          URI or CURIE of procedure to call.
    * @param resultType       Type we want the call result to be converted to.
    * @param resultHandler    Call handler to process call result or error.
    * @param arguments        Call arguments.
    */
   public void call(String procUri, Class<?> resultType, CallHandler resultHandler, Object... arguments) {

      call(procUri, new CallMeta(resultHandler, resultType), arguments);
   }


   /**
    * Issue a remote procedure call (RPC). This version should be used with
    * result types which are containers, i.e. List<> or Map<>.
    *
    * @param procUri          URI or CURIE of procedure to call.
    * @param resultType       Type we want the call result to be converted to.
    * @param resultHandler    Call handler to process call result or error.
    * @param arguments        Call arguments.
    */
   public void call(String procUri, TypeReference<?> resultType, CallHandler resultHandler, Object... arguments) {

      call(procUri, new CallMeta(resultHandler, resultType), arguments);
   }


   /**
    * Subscribe to topic to receive events for.
    *
    * @param topicUri         URI or CURIE of topic to subscribe to.
    * @param meta             Subscription metadata.
    */
   private void subscribe(String topicUri, SubMeta meta) {

      String uri = mOutgoingPrefixes.resolveOrPass(topicUri);

      if (!mSubs.containsKey(uri)) {

         mSubs.put(uri, meta);

         WampMessage.Subscribe msg = new WampMessage.Subscribe(mOutgoingPrefixes.shrink(topicUri));
         mWriter.forward(msg);
      }
   }


   /**
    * Subscribe to topic to receive events for. This version should be used with
    * result types which are containers, i.e. List<> or Map<>.
    *
    * @param topicUri         URI or CURIE of topic to subscribe to.
    * @param eventType        The type we want events to be converted to.
    * @param eventHandler     The event handler to process received events.
    */
   public void subscribe(String topicUri, Class<?> eventType, EventHandler eventHandler) {

      subscribe(topicUri, new SubMeta(eventHandler, eventType));
   }


   /**
    * Subscribe to topic to receive events for.  This version should be used with
    * primitive Java types and simple composite (class) types.
    *
    * @param topicUri         URI or CURIE of topic to subscribe to.
    * @param eventType        The type we want events to be converted to.
    * @param eventHandler     The event handler to process received events.
    */
   public void subscribe(String topicUri, TypeReference<?> eventType, EventHandler eventHandler) {

      subscribe(topicUri, new SubMeta(eventHandler, eventType));
   }


   /**
    * Unsubscribe from topic.
    *
    * @param topicUri      URI or CURIE of topic to unsubscribe from.
    */
   public void unsubscribe(String topicUri) {

      if (mSubs.containsKey(topicUri)) {

         WampMessage.Unsubscribe msg = new WampMessage.Unsubscribe(topicUri);
         mWriter.forward(msg);
         
         mSubs.remove(topicUri);
      }
   }


   /**
    * Unsubscribe from any subscribed topic.
    */
   public void unsubscribe() {

      for (String topicUri : mSubs.keySet()) {

         WampMessage.Unsubscribe msg = new WampMessage.Unsubscribe(topicUri);
         mWriter.forward(msg);
      }
      mSubs.clear();
   }


   /**
    * Establish a prefix to be used in CURIEs.
    *
    * @param prefix     The prefix to be used in CURIEs.
    * @param uri        The full URI this prefix shall resolve to.
    */
   public void prefix(String prefix, String uri) {

      String currUri = mOutgoingPrefixes.get(prefix);

      if (currUri == null || !currUri.equals(uri)) {

         mOutgoingPrefixes.set(prefix, uri);

         WampMessage.Prefix msg = new WampMessage.Prefix(prefix, uri);
         mWriter.forward(msg);
      }
   }


   /**
    * Publish an event to a topic.
    *
    * @param topicUri   URI or CURIE of topic to publish event on.
    * @param event      Event to be published.
    */
   public void publish(String topicUri, Object event) {

      WampMessage.Publish msg = new WampMessage.Publish(mOutgoingPrefixes.shrink(topicUri), event);
      mWriter.forward(msg);
   }
}
