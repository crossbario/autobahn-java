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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import android.os.Handler;
import android.util.Log;
import de.tavendo.autobahn.WampConnection.CallMeta;
import de.tavendo.autobahn.WampConnection.SubMeta;

/**
 * Autobahn WAMP reader, the receiving leg of a WAMP connection.
 */
public class WampReader extends WebSocketReader {

   private static final boolean DEBUG = true;
   private static final String TAG = WampReader.class.getName();

   /// Jackson JSON-to-object mapper.
   private final ObjectMapper mJsonMapper;

   /// Jackson JSON factory from which we create JSON parsers.
   private final JsonFactory mJsonFactory;

   /// Holds reference to call map created on master.
   private final ConcurrentHashMap<String, CallMeta> mCalls;

   /// Holds reference to event subscription map created on master.
   private final ConcurrentHashMap<String, SubMeta> mSubs;

   /**
    * A reader object is created in AutobahnConnection.
    *
    * @param calls         The call map created on master.
    * @param subs          The event subscription map created on master.
    * @param master        Message handler of master (used by us to notify the master).
    * @param socket        The TCP socket.
    * @param options       WebSockets connection options.
    * @param threadName    The thread name we announce.
    */
   public WampReader(ConcurrentHashMap<String, CallMeta> calls,
                         ConcurrentHashMap<String, SubMeta> subs,
                         Handler master,
                         SocketChannel socket,
                         WebSocketOptions options,
                         String threadName) {

      super(master, socket, options, threadName);

      mCalls = calls;
      mSubs = subs;

      mJsonMapper = new ObjectMapper();
      mJsonMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mJsonFactory = mJsonMapper.getJsonFactory();

      if (DEBUG) Log.d(TAG, "created");
   }

   protected void onTextMessage(String payload) {

      /// \todo make error propagation consistent
      notify(new WebSocketMessage.Error(new WebSocketException("non-raw receive of text message")));
   }

   protected void onBinaryMessage(byte[] payload) {

      /// \todo make error propagation consistent
      notify(new WebSocketMessage.Error(new WebSocketException("received binary message")));
   }

   /**
    * Unwraps a WAMP message which is a WebSockets text message with JSON
    * payload conforming to WAMP.
    */
   protected void onRawTextMessage(byte[] payload) {

      try {

         // create parser on top of raw UTF-8 payload
         JsonParser parser = mJsonFactory.createJsonParser(payload);

         // all Autobahn messages are JSON arrays
         if (parser.nextToken() == JsonToken.START_ARRAY) {

            // message type
            if (parser.nextToken() == JsonToken.VALUE_NUMBER_INT) {

               int msgType = parser.getIntValue();

               if (msgType == WampMessage.MESSAGE_TYPE_CALL_RESULT) {

                  // call ID
                  parser.nextToken();
                  String callId = parser.getText();

                  // result
                  parser.nextToken();
                  Object result = null;

                  if (mCalls.containsKey(callId)) {

                     CallMeta meta = mCalls.get(callId);
                     if (meta.mResultClass != null) {
                        result = parser.readValueAs(meta.mResultClass);
                     } else if (meta.mResultTypeRef != null) {
                        result = parser.readValueAs(meta.mResultTypeRef);
                     } else {
                     }
                     notify(new WampMessage.CallResult(callId, result));

                  } else {

                     if (DEBUG) Log.d(TAG, "WAMP RPC success return for unknown call ID received");
                  }

               } else if (msgType == WampMessage.MESSAGE_TYPE_CALL_ERROR) {

                  // call ID
                  parser.nextToken();
                  String callId = parser.getText();

                  // error URI
                  parser.nextToken();
                  String errorUri = parser.getText();

                  // error description
                  parser.nextToken();
                  String errorDesc = parser.getText();

                  if (mCalls.containsKey(callId)) {

                     notify(new WampMessage.CallError(callId, errorUri, errorDesc));

                  } else {

                     if (DEBUG) Log.d(TAG, "WAMP RPC error return for unknown call ID received");
                  }

               } else if (msgType == WampMessage.MESSAGE_TYPE_EVENT) {

                  // topic URI
                  parser.nextToken();
                  String topicUri = parser.getText();

                  // event
                  parser.nextToken();
                  Object event = null;

                  if (mSubs.containsKey(topicUri)) {

                     SubMeta meta = mSubs.get(topicUri);
                     if (meta.mEventClass != null) {
                        event = parser.readValueAs(meta.mEventClass);
                     } else if (meta.mEventTypeRef != null) {
                        event = parser.readValueAs(meta.mEventTypeRef);
                     } else {
                     }
                     notify(new WampMessage.Event(topicUri, event));

                  } else {

                     if (DEBUG) Log.d(TAG, "WAMP event for not-subscribed topic received");
                  }

               } else if (msgType == WampMessage.MESSAGE_TYPE_PREFIX) {

                  // prefix
                  parser.nextToken();
                  String prefix = parser.getText();

                  // URI
                  parser.nextToken();
                  String uri = parser.getText();

                  notify(new WampMessage.Prefix(prefix, uri));

               } else if (msgType == WampMessage.MESSAGE_TYPE_WELCOME) {

                  // session ID
                  parser.nextToken();
                  String sessionId = parser.getText();

                  // protocol version
                  parser.nextToken();
                  int protocolVersion = parser.getIntValue();

                  // server ident
                  parser.nextToken();
                  String serverIdent = parser.getText();

                  notify(new WampMessage.Welcome(sessionId, protocolVersion, serverIdent));

               } else {

                  // FIXME: invalid WAMP message
                  if (DEBUG) Log.d(TAG, "invalid WAMP message: unrecognized message type");

               }
            } else {

               if (DEBUG) Log.d(TAG, "invalid WAMP message: missing message type or message type not an integer");
            }

            if (parser.nextToken() == JsonToken.END_ARRAY) {

               // nothing to do here

            } else {

               if (DEBUG) Log.d(TAG, "invalid WAMP message: missing array close or invalid additional args");
            }

         } else {

            if (DEBUG) Log.d(TAG, "invalid WAMP message: not an array");
         }
         parser.close();


      } catch (JsonParseException e) {

         if (DEBUG) e.printStackTrace();

      } catch (IOException e) {

         if (DEBUG) e.printStackTrace();

      }
   }
}
