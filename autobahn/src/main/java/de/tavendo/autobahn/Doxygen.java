package de.tavendo.autobahn;

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
<a href="https://github.com/crossbario/autobahn-android">code repository</a>.
*/

/// Empty class file to hold Doxygen documentation.
abstract class Doxygen {

}
