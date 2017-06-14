# Autobahn|Java

## **Under Development**

Autobahn|Java is a subproject of the [Autobahn project](http://crossbar.io/autobahn/) and provides open-source client implementations for

* **[The WebSocket Protocol](http://tools.ietf.org/html/rfc6455)**
* **[The Web Application Messaging Protocol (WAMP)](http://wamp.ws/)**

running on Android/Dalvik and Netty/JVM.

The WebSocket layer is using a callback based user API.

The WAMP layer is using Java CompletableFuture for WAMP actions (call, register, publish and subscribe) and the Observer pattern for WAMP session, subscription and registration lifecycle events.

The library is MIT licensed, maintained by the Crossbar.io Project, tested using the AutobahnTestsuite and published as a JAR to Maven and as a Docker toolchain image to Dockerhub.


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

## Get in touch

Get in touch on IRC #autobahn on chat.freenode.net or join the [mailing list](http://groups.google.com/group/autobahnws).
