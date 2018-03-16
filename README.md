# **Autobahn**|Java

Client library providing [WAMP](http://wamp-proto.org/) on Java 8 ([Netty](https://netty.io/)) and Android, plus (secure) WebSocket for Android.

[![Docker Hub](https://img.shields.io/badge/docker-ready-blue.svg)](https://hub.docker.com/r/crossbario/autobahn-java/) |
[![Travis](https://travis-ci.org/crossbario/autobahn-java.svg?branch=master)](https://travis-ci.org/crossbario/autobahn-java)
[![Docs](https://javadoc.io/badge/io.crossbar.autobahn/autobahn-android.svg?label=docs)](https://javadoc.io/doc/io.crossbar.autobahn/autobahn-android)

---

Autobahn|Java is a subproject of the [Autobahn project](http://crossbar.io/autobahn/) and provides open-source client implementations for

* **[The WebSocket Protocol](http://tools.ietf.org/html/rfc6455)**
* **[The Web Application Messaging Protocol (WAMP)](http://wamp-proto.org/)**

running on Android and Netty/Java8/JVM.

The WebSocket layer is using a callback based user API, and is specifically written for Android. Eg it does not run any network stuff on the main (UI) thread.

The WAMP layer is using Java 8 **[CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)** for WAMP actions (call, register, publish and subscribe) and the **[Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern)** for WAMP session, subscription and registration lifecycle events.

The library is MIT licensed, maintained by the Crossbar.io Project, tested using the AutobahnTestsuite and published as a JAR to Maven and as a Docker toolchain image to Dockerhub.

---


## Download

Grab via Maven:

```xml
<dependency>
    <groupId>io.crossbar.autobahn</groupId>
    <artifactId>autobahn-android</artifactId>
    <version>(insert latest version)</version>
</dependency>
```

Gradle:
```groovy
dependencies {
    implementation 'io.crossbar.autobahn:autobahn-android:17.10.5'
}
```
For non-android systems use artifactID `autobahn-java` or just
Download [the latest JAR](https://search.maven.org/remote_content?g=io.crossbar.autobahn&a=autobahn-java&v=LATEST)


## Getting Started

The demo clients are easy to run, you only need `make` and `docker` installed to get things rolling. </br>

    $ make crossbar # Starts crossbar in a docker container
    $ make python # Starts a python based WAMP components that provides calls for the Java demo client

and finally

    $ make java # Starts the java (Netty) based demo client that performs WAMP actions

## Show me some code

The code in demo-gallery contains some examples on how to use the autobahn library, it also contains convenience methods to use. Below is a basic set of code examples showing all 4 WAMP actions.

### Subscribe to a topic

```java
public void demonstrateSubscribe(Session session, SessionDetails details) {
    // Subscribe to topic to receive its events.
    CompletableFuture<Subscription> subFuture = session.subscribe("com.myapp.hello",
            this::onEvent);
    subFuture.whenComplete((subscription, throwable) -> {
        if (throwable == null) {
            // We have successfully subscribed.
            System.out.println("Subscribed to topic " + subscription.topic);
        } else {
            // Something went bad.
            throwable.printStackTrace();
        }
    });
}

private void onEvent(List<Object> args, Map<String, Object> kwargs, EventDetails details) {
    System.out.println(String.format("Got event: %s", args.get(0)));
}
```
Since we are only accessing `args` in onEvent(), we could simplify it like:
```java
private void onEvent(List<Object> args) {
    System.out.println(String.format("Got event: %s", args.get(0)));
}
```
### Publish to a topic

```java
public void demonstratePublish(Session session, SessionDetails details) {
    // Publish to a topic that takes a single arguments
    List<Object> args = Arrays.asList("Hello World!", 900, "UNIQUE");
    CompletableFuture<Publication> pubFuture = session.publish("com.myapp.hello", args);
    pubFuture.thenAccept(publication -> System.out.println("Published successfully"));
    // Shows we can separate out exception handling
    pubFuture.exceptionally(throwable -> {
        throwable.printStackTrace();
        return null;
    });
}
```
A simpler call would look like:
```java
public void demonstratePublish(Session session, SessionDetails details) {
    CompletableFuture<Publication> pubFuture = session.publish("com.myapp.hello", "Hi!");
    ...
}
```

### Register a procedure

```java
public void demonstrateRegister(Session session, SessionDetails details) {
    // Register a procedure.
    CompletableFuture<Registration> regFuture = session.register("com.myapp.add2", this::add2);
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
A very precise `add2` may look like:
```java
private List<Object> add2(List<Integer> args, InvocationDetails details) {
    int res = args.get(0) + args.get(1);
    return Arrays.asList(res, details.session.getID(), "Java");
}
```

### Call a procedure

```java
public void demonstrateCall(Session session, SessionDetails details) {
    // Call a remote procedure.
    CompletableFuture<CallResult> callFuture = session.call("com.myapp.add2", 10, 20);
    callFuture.thenAccept(callResult ->
            System.out.println(String.format("Call result: %s", callResult.results.get(0))));
}
```

### Connecting the dots

```java
public void main() {
    // Create a session object
    Session session = new Session();
    // Add all onJoin listeners
    session.addOnJoinListener(this::demonstrateSubscribe);
    session.addOnJoinListener(this::demonstratePublish);
    session.addOnJoinListener(this::demonstrateCall);
    session.addOnJoinListener(this::demonstrateRegister);

    // finally, provide everything to a Client and connect
    Client client = new Client(session, url, realm);
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
