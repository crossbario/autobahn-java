..


|ab| Documentation
==================

.. container:: legacynotice

   |ab| implements version 1 of WAMP.

   This is incompatible with version 2 of WAMP which is already implemented in Autobahn|Python as well as Autobahn|JS

   Migration of |ab| to WAMP v2 is coming, but we cannot guarantee a release date.

|ab| is an open-source networking library for Java/`Android <http://developer.android.com/>`_ created by the `Autobahn project <http://autobahn.ws/>`_ that implements

* the `WebSocket Protocol <http://tools.ietf.org/html/rfc6455>`_ and
* the `Web Application Messaging Protocol (WAMP) <http://wamp.ws/>`_

for creating native mobile WebSocket/WAMP clients.


WebSocket
---------

 * supports WebSocket RFC6455, Draft Hybi-10+ and WAMP v1
 * works on Android 2.2+
 * very good `standards conformance <http://autobahn.ws/testsuite/>`_
 * high-performance asynchronous design
 * easy to use API
 * seamless integration in Android UI apps
 * no (really none) network activity on UI thread
 * `Open-source <https://github.com/tavendo/AutobahnAndroid>`_ (Apache 2 license)

You can use |ab| to create native Android apps talking to WebSocket servers or any `WAMP <http://wamp.ws/>`_ compatible server.

Here is a simple WebSocket echo client:

.. code-block:: java

   private final WebSocketConnection mConnection = new WebSocketConnection();

   private void start() {

      final String wsuri = "ws://localhost:9000";

      try {
         mConnection.connect(wsuri, new WebSocketHandler() {

            @Override
            public void onOpen() {
               Log.d(TAG, "Status: Connected to " + wsuri);
               mConnection.sendTextMessage("Hello, world!");
            }

            @Override
            public void onTextMessage(String payload) {
               Log.d(TAG, "Got echo: " + payload);
            }

            @Override
            public void onClose(int code, String reason) {
               Log.d(TAG, "Connection lost.");
            }
         });
      } catch (WebSocketException e) {

         Log.d(TAG, e.toString());
      }
   }

Complete Tutorial:    `WebSocket Echo Client <https://github.com/tavendo/AutobahnAndroid/tree/master/Demo/EchoClient>`_


Web Application Messaging Protocol (WAMP)
-----------------------------------------

|ab| also includes an implementation of the `Web Application Messaging Protocol (WAMP) <http://wamp.ws/>`_ (version 1) which can be used to build applications around Remote Procedure Call and Publish & Subscribe messaging patterns.

It features:

 * RPC and PubSub, fully asynchronous design
 * built on JSON and WebSockets
 * simple, efficient and open protocol
 * automatic mapping to user-defined POJOs
 * seamless integration in Android UI apps
 * Open-Source, licensed under the Apache 2.0 license
 * Call results and events which travel the wire as JSON payload are automatically converted and mapped to Java primitive types or user-defined POJOs (Plain-old Java Objects).

The latter is a very convenient and powerful feature made possible by the use of Jackson, a high-performance JSON processor. This works even for container types, such as lists or maps over POJOs.

For example, it is possible to issue a RPC and get a List<Person> as a result, where Person is a user-defined class.

WAMP allows to develop real-time enabled apps based on asynchronous RPC and PubSub messaging patterns.

Here is a simple WAMP client calling a RPC endpoint and subscribing to a PubSub topic:

.. code-block:: java

   private final AutobahnConnection mConnection = new AutobahnConnection();

   private void start() {

      final String wsuri = "ws://localhost:9000";

      mConnection.connect(wsuri, new Autobahn.SessionHandler() {

         @Override
         public void onOpen() {
            testRpc();
            testPubSub();
         }

         @Override
         public void onClose(int code, String reason) {
         }
      });
   }

   private void testRpc() {

      mConnection.call("http://example.com/calc#add",
                       Integer.class,
                       new Autobahn.CallHandler() {

                           @Override
                           public void onResult(Object result) {
                              int res = (Integer) result;
                              Log.d(TAG, "calc:add result = " + res);
                           }

                           @Override
                           public void onError(String error, String info) {
                           }
                       },
                       23, 55
      );
   }

   private static class MyEvent1 {
      public int num;
      public String name;
      public boolean flag;
      public Date created;
      public double rand;
   }

   private void testPubSub() {

      mConnection.subscribe("http://example.com/events#myevent1",
                            MyEvent1.class,
                            new Autobahn.EventHandler() {

                               @Override
                               public void onEvent(String topic, Object event) {

                                  MyEvent1 evt = (MyEvent1) event;
                               }
                            }
      );
   }

Complete Tutorials:   `WAMP RPC Client <https://github.com/tavendo/AutobahnAndroid/tree/master/Demo/SimpleRpc>`_   `WAMP PubSub Client <https://github.com/tavendo/AutobahnAndroid/tree/master/Demo/SimplePubSub>`_

You can use any Java basic type or POJO with RPC and PubSub and have i.e. PubSub event payloads automatically mapped to your POJOs.


If you like, :doc:`get started <gettingstarted>`, check out the :doc:`examples` and - for complete information - the :doc:`API reference </_gen/packages>`.



.. toctree::
   :maxdepth: 2
   :hidden:

   index
   gettingstarted
   examples
   _gen/packages
   table_of_contents


Indices and tables
==================

* :ref:`genindex`
* :ref:`search`

