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

/*!
\mainpage
\section intro_sec AutobahnAndroid API Reference

<a href="http://autobahn.ws">AutobahnAndroid</a> provides a Java client library implementing
<a href="http://tools.ietf.org/html/rfc6455">The WebSocket Protocol</a> and
<a href="http://wamp.ws">The WebSocket Application Messaging Protocol</a> for use
in native Android apps.


\section websocket_features WebSocket Support

<a href="http://autobahn.ws">AutobahnAndroid</a> implements the WebSocket protocol
with a couple of distinct features:

\li full RFC6455 and Draft Hybi-10 to -17 support
\li very good <a href="http://autobahn.ws/testsuite/reports/mobileclients/">standards conformance</a>
\li performant
\li easy to use API
\li designed to work with Android UI applications
\li Open-Source, licensed under the Apache 2.0 license

The implementation passes all (nearly 300) tests from the
<a href="http://autobahn.ws/testsuite">AutobahnTestSuite</a>.

The basic API is modeled after the WebSocket JavaScript API for
ease of use and familarity.

The API enables the use of common Android idioms for event handling (using
anonymous inner classes) and integrates with Android UI applications (by
communicating via messages and message loops between the UI thread and back-
ground reader/writer threads and by avoiding _any_ network activity on the
UI thread).

The implementation uses Java NIO to reduce network processing overhead and
is on-par or faster performance-wise compared to Firefox 8 Mobile, a C++
implementation of WebSockets.

\section rpc_pubsub WAMP (RPC/PubSub) Support

<a href="http://autobahn.ws">AutobahnAndroid</a> also
includes an implementation of <a href="http://wamp.ws">The WebSocket Application Messaging Protocol (WAMP)</a>
which can be used to build applications around <b>Remote Procedure Call</b> and
<b>Publish & Subscribe</b> messaging patterns.

It features:

\li RPC and PubSub, fully asynchronous design
\li built on JSON and WebSockets
\li simple, efficient and open protocol
\li automatic mapping to user-defined POJOs
\li seamless integration in Android UI apps
\li Open-Source, licensed under the Apache 2.0 license

Call results and events which travel the wire as JSON payload are automatically
converted and mapped to Java primitive types or user-defined POJOs (Plain-old Java Objects).

The latter is a very convenient and powerful feature made possible by the use of
<a href="http://jackson.codehaus.org/">Jackson</a>, a high-performance JSON processor.
This works even for container types, such as lists or maps over POJOs.

For example, it is possible to issue a RPC and get a List<Person> as a result, where Person is
a user-defined class.

\section usage Usage

The only dependency of
<a href="http://autobahn.ws">AutobahnAndroid</a>
is <a href="http://jackson.codehaus.org/">Jackson</a>.
To use, all one needs to do is to include the built JARs into an Android
project.

\section more More Information

For more information, please visit the <a href="http://autobahn.ws/android">project page</a>,
the <a href="http://groups.google.com/group/autobahnws">forum</a> or the
<a href="https://github.com/tavendo/AutobahnAndroid">code repository</a>.
Commercial support and services is available from <a href="http://www.tavendo.de">Tavendo GmbH</a>.
*/

/**
 * Autobahn interface.
 */
public interface Wamp {

   /**
    * Session handler for Autobahn sessions.
    */
   public interface SessionHandler {

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
   public void connect(String wsUri, SessionHandler sessionHandler);


   /**
    * Connect to Autobahn server.
    *
    * @param wsUri            The WebSockets URI of the server.
    * @param sessionHandler   The handler for the session.
    * @param options          WebSockets and Autobahn option.s
    */
   public void connect(String wsUri, SessionHandler sessionHandler, WampOptions options);


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
    * Call handler.
    */
   public interface CallHandler {

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
    * @param callHandler   The handler to be invoked upon call completion.
    * @param arguments     Zero, one or more arguments for the call.
    */
   public void call(String procUri, Class<?> resultType, CallHandler callHandler, Object... arguments);

   /**
    * Call a remote procedure (RPC).
    *
    * @param procUri       The URI or CURIE of the remote procedure to call.
    * @param resultType    The type the call result gets transformed into.
    * @param callHandler   The handler to be invoked upon call completion.
    * @param arguments     Zero, one or more arguments for the call.
    */
   public void call(String procUri, TypeReference<?> resultType, CallHandler callHandler, Object... arguments);

   /**
    * Handler for PubSub events.
    */
   public interface EventHandler {

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
   public void subscribe(String topicUri, Class<?> eventType, EventHandler eventHandler);

   /**
    * Subscribe to a topic. When already subscribed, overwrite the event handler.
    *
    * @param topicUri      The URI or CURIE of the topic to subscribe to.
    * @param eventType     The type that event get transformed into.
    * @param eventHandler  The event handler.
    */
   public void subscribe(String topicUri, TypeReference<?> eventType, EventHandler eventHandler);

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
