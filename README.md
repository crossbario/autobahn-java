# **Autobahn**|Java

Client library providing [WAMP](http://wamp-proto.org/) on Java 8 ([Netty](https://netty.io/)) and Android, plus (secure) WebSocket for Android.

[![Docker Hub](https://img.shields.io/badge/docker-ready-blue.svg)](https://hub.docker.com/r/crossbario/autobahn-java/) |
[![Travis](https://travis-ci.org/crossbario/autobahn-java.svg?branch=master)](https://travis-ci.org/crossbario/autobahn-java)
[![Docs](https://img.shields.io/badge/Docs-latest-ff69b4.svg)](https://crossbario.github.io/autobahn-java-docs/)

---

Autobahn|Java is a subproject of the [Autobahn project](http://crossbar.io/autobahn/) and provides open-source client implementations for

* **[The WebSocket Protocol](http://tools.ietf.org/html/rfc6455)**
* **[The Web Application Messaging Protocol (WAMP)](http://wamp-proto.org/)**

running on Android and Netty/Java8/JVM.

The WebSocket layer is using a callback based user API, and is specifically written for Android. Eg it does not run any network stuff on the main (UI) thread.

The WAMP layer is using Java 8 **[CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)** for WAMP actions (call, register, publish and subscribe) and the **[Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern)** for WAMP session, subscription and registration lifecycle events.

The library is MIT licensed, maintained by the Crossbar.io Project, tested using the AutobahnTestsuite and published as a JAR to Maven and as a Docker toolchain image to Dockerhub.

---


## Getting Started

### WAMP on Netty

```java
    public void onJoinHandler(Session session, SessionDetails details) {
        // Subscribe to topic to receive its events.
        CompletableFuture<Subscription> subFuture = session.subscribe(
                "com.myapp.hello", this::onEvent, null);

        // Publish to a topic.
        List<Object> pubItems = new ArrayList<>();
        pubItems.add("Hello World!");
        CompletableFuture<Publication> pubFuture = session.publish(
                "com.myapp.hello", pubItems, null, null);

        // Register a procedure.
        CompletableFuture<Registration> regFuture = session.register(
                "com.myapp.add2", this::add2, null);

        // Call a remote procedure.
        List<Object> callArgs = new ArrayList<>();
        callArgs.add(10);
        callArgs.add(20);
        CompletableFuture<CallResult> callFuture = session.call(
                "com.myapp.add2", callArgs, null, null);
        callFuture.thenAccept(callResult ->
                System.out.println(String.format("Call result: %s", callResult.results.get(0))));
    }

    private void onEvent(List<Object> args, Map<String, Object> kwargs, EventDetails details) {
        System.out.println(String.format("Got event: %s", args.get(0)));
    }

    private CompletableFuture<InvocationResult> add2(List<Object> args, Map<String, Object> kwargs,
                                                     InvocationDetails details) {
        int res = (int) args.get(0) + (int) args.get(1);
        List<Object> arr = new ArrayList<>();
        arr.add(res);
        return CompletableFuture.completedFuture(new InvocationResult(arr));
    }
```


### WebSocket on Android

TBD

---


## Get in touch

Get in touch on IRC #autobahn on chat.freenode.net or join the [mailing list](http://groups.google.com/group/autobahnws).

---


## Version 1

Version 1 of this library is still in the repo [here](https://github.com/crossbario/autobahn-java/tree/version-1), but is no longer maintained.

Version 1 only supported non-secure WebSocket on Android and only supported WAMP v1.

Both of these issues are fixed in the (current) version of Autobahn|Java.

---
