# **Autobahn**|Java

Client library providing [WAMP](http://wamp-proto.org/) on Java 8 ([Netty](https://netty.io/)) and Android, plus (secure) WebSocket for Android.

[![Docker Hub](https://img.shields.io/badge/docker-ready-blue.svg)](https://hub.docker.com/r/crossbario/autobahn-java/) |
[![Travis](https://travis-ci.org/crossbario/autobahn-java.svg?branch=master)](https://travis-ci.org/crossbario/autobahn-java)
[![Docs](https://javadoc.io/badge/io.crossbar.autobahn/autobahn-android.svg)](https://javadoc.io/doc/io.crossbar.autobahn/autobahn-android)

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

The demo clients are easy to run, you only need `make` and `docker` installed to get things rolling. </br>

    $ make crossbar # Starts crossbar in a docker container
    $ make python # Starts a python based WAMP components that provides calls for the Java demo client

and finally

    `$ make java` # Starts the java (Netty) based demo client that performs WAMP actions

### Show me some code

The code in demo-gallery contains some examples on how to use the autobahn library, it also contains convenience methods to use. Below is a basic set of code examples showing all 4 WAMP actions.

Subscribe to a topic

```java
public void demonstrateSubscribe(Session session, SessionDetails details) {
    // Subscribe to topic to receive its events.
    CompletableFuture<Subscription> subFuture = session.subscribe(
            "com.myapp.hello", this::onEvent, null);
    subFuture.thenAccept(subscription -> {
        // We have successfully subscribed.
        System.out.println("Subscribed to topic " + subscription.topic);
    });
}

private void onEvent(List<Object> args, Map<String, Object> kwargs, EventDetails details) {
    System.out.println(String.format("Got event: %s", args.get(0)));
}
```

Publish to a topic

```java
public void demonstratePublish(Session session, SessionDetails details) {
    // Publish to a topic.
    List<Object> pubItems = new ArrayList<>();
    pubItems.add("Hello World!");
    CompletableFuture<Publication> pubFuture = session.publish(
            "com.myapp.hello", pubItems, null, null);
    pubFuture.thenAccept(publication -> System.out.println("Publisheded successfully"));
}
```

Register a procedure

```java
public void demonstrateRegister(Session session, SessionDetails details) {
    // Register a procedure.
    CompletableFuture<Registration> regFuture = session.register(
            "com.myapp.add2", this::add2, null);
    regFuture.thenAccept(registration ->
            System.out.println("Successfully registered procedure: " + registration.procedure));
}

private CompletableFuture<InvocationResult> add2(
        List<Object> args, Map<String, Object> kwargs, InvocationDetails details) {
    int res = (int) args.get(0) + (int) args.get(1);
    List<Object> arr = new ArrayList<>();
    arr.add(res);
    return CompletableFuture.completedFuture(new InvocationResult(arr));
}
```

Call a procedure

```java
public void demonstrateCall(Session session, SessionDetails details) {
    // Call a remote procedure.
    List<Object> callArgs = new ArrayList<>();
    callArgs.add(10);
    callArgs.add(20);
    CompletableFuture<CallResult> callFuture = session.call(
            "com.myapp.add2", callArgs, null, null);
    callFuture.thenAccept(callResult ->
            System.out.println(String.format("Call result: %s", callResult.results.get(0))));
}
```

Connecting the dots

```java
public void main() {
    // Create a session object
    Session session = new Session();
    // Add all onJoin listeners
    session.addOnJoinListener(this::demonstrateSubscribe);
    session.addOnJoinListener(this::demonstratePublish);

    // Now create a transport list to try and add transports to it.
    // In our case, we currnetly only have Netty based WAMP-over-WebSocket.
    List<ITransport> transports = new ArrayList<>();
    transports.add(new NettyTransport(websocketURL));

    // Now provide a list of authentication methods.
    // We only support anonymous auth currently.
    List<IAuthenticator> authenticators = new ArrayList<>();
    authenticators.add(new AnonymousAuth());

    // finally, provide everything to a Client instance and connect
    Client client = new Client(transports);
    client.add(session, realm, authenticators);
    CompletableFuture<ExitInfo> exitInfoCompletableFuture = client.connect();
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
