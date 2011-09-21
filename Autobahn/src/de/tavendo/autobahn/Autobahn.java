/******************************************************************************
 *
 *  Copyright 2011 Tavendo GmbH
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


import org.codehaus.jackson.type.TypeReference;

public interface Autobahn {

   /**
    * Session handler for Autobahn sessions.
    */
   public interface OnSession {

      /**
       * Connection was closed normally.
       */
      public static final int CLOSE_NORMAL = 1;

      /**
       * Connection could not be established in the first place.
       */
      public static final int CLOSE_CANNOT_CONNECT = 2;

      /**
       * A previously established connection was lost unexpected.
       */
      public static final int CLOSE_CONNECTION_LOST = 3;

      /**
       * The connection was closed because a protocol violation
       * occurred.
       */
      public static final int CLOSE_PROTOCOL_ERROR = 4;

      /**
       * Fired upon successful establishment of connection to Autobahn server.
       */
      public void onOpen();

      /**
       * Firex upon unsuccessful connection attempt or when connection
       * was closed normally, or abnormally.
       *
       * @param code       The close code, which provides information about why the connection was closed.
       * @param reason     A humand readable description of the reason of connection close.
       */
      public void onClose(int code, String reason);

   }

   /**
    * Connect to Autobahn server.
    *
    * @param wsUri            The WebSockets URI of the server.
    * @param sessionHandler   The handler for the session.
    */
   public void connect(String wsUri, OnSession sessionHandler);


   /**
    * Disconnect from Autobahn server.
    */
   public void disconnect();

   /**
    * Check if currently connected to server.
    *
    * @return     True, iff connected.
    */
   public boolean isConnected();


   /**
    * Establish a prefix to be used in CURIEs to shorten URIs.
    *
    * @param prefix           The prefix to be used in CURIEs.
    * @param uri              The full URI this prefix shall resolve to.
    */
   public void prefix(String prefix, String uri);

   /**
    * Handler for RPC results.
    */
   public interface OnCallResult {

      /**
       * Fired on successful completion of call.
       *
       * @param result     The RPC result transformed into the type that was specified in call.
       */
      public void onResult(Object result);

      /**
       * Fired on call failure.
       *
       * @param errorUri   The URI or CURIE of the error that occurred.
       * @param errorDesc  A human readable description of the error.
       */
      public void onError(String errorUri, String errorDesc);
   }

   /**
    * Call a remote procedure (RPC).
    *
    * @param procUri       The URI or CURIE of the remote procedure to call.
    * @param resultType    The type the call result gets transformed into.
    * @param resultHandler The handler to be invoked upon call completion.
    * @param arguments     Zero, one or more arguments for the call.
    */
   public void call(String procUri, Class<?> resultType, OnCallResult resultHandler, Object... arguments);

   /**
    * Call a remote procedure (RPC).
    *
    * @param procUri       The URI or CURIE of the remote procedure to call.
    * @param resultType    The type the call result gets transformed into.
    * @param resultHandler The handler to be invoked upon call completion.
    * @param arguments     Zero, one or more arguments for the call.
    */
   public void call(String procUri, TypeReference<?> resultType, OnCallResult resultHandler, Object... arguments);

   /**
    * Handler for PubSub events.
    */
   public interface OnEventHandler {

      /**
       * Fired when an event for the PubSub subscription is received.
       *
       * @param topicUri   The URI or CURIE of the topic the event was published to.
       * @param event      The event, transformed into the type that was specified when subscribing.
       */
      public void onEvent(String topicUri, Object event);
   }

   /**
    * Subscribe to a topic. When already subscribed, overwrite the event handler.
    *
    * @param topicUri      The URI or CURIE of the topic to subscribe to.
    * @param eventType     The type that event get transformed into.
    * @param eventHandler  The event handler.
    */
   public void subscribe(String topicUri, Class<?> eventType, OnEventHandler eventHandler);

   /**
    * Subscribe to a topic. When already subscribed, overwrite the event handler.
    *
    * @param topicUri      The URI or CURIE of the topic to subscribe to.
    * @param eventType     The type that event get transformed into.
    * @param eventHandler  The event handler.
    */
   public void subscribe(String topicUri, TypeReference<?> eventType, OnEventHandler eventHandler);

   /**
    * Unsubscribe from given topic.
    *
    * @param topicUri      The URI or CURIE of the topic to unsubscribe from.
    */
   public void unsubscribe(String topicUri);

   /**
    * Unsubscribe from any topics subscribed.
    */
   public void unsubscribe();

   /**
    * Publish an event to the specified topic.
    *
    * @param topicUri      The URI or CURIE of the topic the event is to be published for.
    * @param event         The event to be published.
    */
   public void publish(String topicUri, Object event);

}
