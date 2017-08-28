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

---


### WebSocket on Android

Write me.

---


## Get in touch

Get in touch on IRC #autobahn on chat.freenode.net or join the [mailing list](http://groups.google.com/group/autobahnws).

---


## Version 1

Version 1 of this library is still in the repo [here](https://github.com/crossbario/autobahn-java/tree/version-1), but is no longer maintained.

Version 1 only supported non-secure WebSocket on Android and only supported WAMP v1.

Both of these issues are fixed in the (current) version of Autobahn|Java.

---
