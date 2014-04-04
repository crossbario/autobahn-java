# AutobahnAndroid

**Autobahn**|Android is a subproject of the [Autobahn project](http://autobahn.ws/) and provides an open-source implementation

* **[The WebSocket Protocol](http://tools.ietf.org/html/rfc6455)**
* **[The Web Application Messaging Protocol (WAMP)](http://wamp.ws/)**

for Android.

WebSocket allows [bidirectional real-time messaging on the Web](http://tavendo.com/blog/post/websocket-why-what-can-i-use-it/).

WAMP provides asynchronous **Remote Procedure Calls** and **Publish & Subscribe** for applications in *one* protocol.

WAMP is ideal for distributed, multi-client and server applications, such as multi-user database-drive business applications, sensor networks (IoT), instant messaging or MMOGs (massively multi-player online games) .

> Note: **Autobahn**|Android implements version 1 of WAMP. Current versions of the other Autobahn project libraries already provide implementations of version 2 of the protocol, with substantially expanded capabilities. An update to **Autobahn**|Android is under development.

## Show me some code

Here is a simple WebSocket echo client:

```java
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
```

... and a simple WAMP v1 client calling a RPC endpoint and subscribitng to a PubSub topic:

```java
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
```




## Features

* library for WebSocket and WAMP clients
* implements WebSocket RFC6455, Draft Hybi-10+ and WAMP v1
* works on Android 2.2+
* very good standards conformance
* high-performance asynchronous design
* easy to use API
* seamless integration in Android UI apps
* no (really none) network activity on UI thread
* Open-source (Apache 2 license)

You can use AutobahnAndroid to create native Android apps talking to WebSocket servers or any WAMP compatible server.

## More Information

For more information, take a look at the [project documentation](http://autobahn.ws/android). This provides:

* [a quick 'Getting Started'](http://autobahn.ws/android/gettingstarted.html)
* [a list of all examples in this repo](http://autobahn.ws/android/examples.html)
* [a full API reference](http://autobahn.ws/python/packages.html)


## Get in touch

Get in touch on IRC #autobahn on chat.freenode.net or join the [mailing list](http://groups.google.com/group/autobahnws).
