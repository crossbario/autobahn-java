## Observing Session lifecycle events

To observe events occuring in the lifecycle of a WAMP session, and run user code
when the state of the WAMP session changes to one of the following states:

* CONNECTED
* JOINED
* READY
* LEFT
* DISCONNECTED

With future resumable session, there might also be states such as:

* CONNECTED_RESUMABLE
* DISCONNECTED_RESUMABLE
* JOINED_RESUMING

Here is an example:


```java
/**
 * Listener for observing the CONNECTED state of a session.
 *
 * This is modeled following this https://dzone.com/articles/the-observer-pattern-using-modern-java
 */
public interface ConnectedListener {
    public void onChannelConnected (MessageChannel channel);
}

public class Main {

    public static void main (String[] args) {

        // create a WAMP session object on the green field
        Session session = new Session();

        // register a listener when the session was connected to a channel
        // here, we a) use a lambda function to shorten the code we have to write,
        // and b) keep a reference to the listener created and returned around
        // so we can c) later explicitly unregister the listener. note that b) is
        // only needed if you want to do c).
        ConnectedListener listener = session.registerConnectedListener(

            (channel) -> System.out.println(
                "Session connected to WAMP message channel:\n" +
                "protocol='" + channel.protocol.getName() + "'\n" +
                "serializer='" + channel.serializer.getName() + "'"
            )
        );

        // assume "channel" is a WAMP channel object created magically
        // and connect the session to the channel
        session.connect(channel);

        // explicitly unregister the listener
        session.unregisterConnectedListener(listener);

        // now disconnect the session from the current channel (because otherwise,
        // we can't connect it to a different one - see below)
        session.disconnect("wamp.exit.normal", "Bye, bye! Will be back later.");

        // now connect the session to a different channel. this will NOT fire
        // our listener as we have explicitly unregister that listener
        session.connect(channel2);
    }
}
```

## WAMP Session

```console
public class Session {

    void connect(Channel channel);

    Channel get_channel();

    void disconnect();

    int get_state() {
        // CONNECTED
        // CONNECTED_RESUMING
        // DISCONNECTED
        // DISCONNECTED_RESUMABLE
        // HELLO_SENT
        // AUTHENTICATE_SENT
        // JOINED
        // READY
        // GOODBYE_SENT
        // ABORT_SENT
    }
}
```


## WAMP Actions

```console
public class Session {

    /**
     * Publish an event to a topic.
     *
     * @param topic     The URI of the topic to publish on.
     * @param args      Positional arguments (payload) of the event publish.
     * @param kwargs    Keyword arguments (payload) of the event to publish.
     * @param options   Publication options.
     * @return          A CompletableFuture that resolves to an instance of Publication on success.
     */
    CompletableFuture<Publication> publish(String topic,
                                           List<Object> args,
                                           Map<String, Object> kwargs,
                                           PublishOptions options);

    CompletableFuture<Subscription> subscribe(String topic,
                                              SubscribeOptions options);

    /**
     * Issue a call to a procedure.
     *
     * @param procedure     The URI of the procedure to call.
     * @param args          Positional arguments (payload) to the call issued.
     * @param kwargs        Keyword arguments (payload) to the call issued.
     * @return              A CompletableFuture that resolves to an instance of CallResult on success.
     */
    CompletableFuture<CallResult> call(String procedure,
                                       List<Object> args,
                                       Map<String, Object> kwargs,
                                       CallOptions options);

    CompletableFuture<Registration> register(String procedure,
                                             RegisterOptions options);
}
```

## Basic Usage

```java
public class Main {

    public static void main (String[] args) {

        // assume WAMP message channel and session are created magically
        MessageChannel channel;
        Session session;

        // then, define a sequence of authentication requests
        List<AuthRequest> auth_requests = new List([new AnonymousAuthRequest("anonymous", "realm1")]);

        // register a listener for the session becoming CONNECTED
        session.registerConnectedListener(

            // when the session becomes CONNECTED, trigger joining ..
            (connected_details) -> session.join(auth_requests)
        );

        // register a listener for the session becoming JOINED on a realm
        session.registerJoinedListener(

            // when the session becomes JOINED, register a procedure ..
            (joined_details) -> session.register(
                "com.example.add2",
                (args, kwargs, details) -> return (args[0] + args[1], null, null)
            )
        );

        // register a listener for the session becoming READY
        session.registerReadyListener(

            // when the session becomes READY, log a message.
            (ready_details) -> System.out.println("component ready: " + ready_details.to_string())
        );

        // now actually connect the session to the channel, which will then
        // trigger of a cascade of events and event handling code as above
        session.connect(channel);
    }
}
```

## Handling Auto-reconnect

The following shows a rough sketch of how user controller auto-reconnect of a (disconnected, previously connected) message channel might look like.

The user code register a listener for observing the session moving into the DISCONNECTED state. When that happens, the user connect will trigger a reconnect on the original message channel

```java
public class Main {

    public static void main (String[] args) {

        // assume WAMP message channel and session are created magically
        MessageChannel channel;
        Session session;

        // then, define a sequence of authentication requests
        List<AuthRequest> auth_requests = new List([new AnonymousAuthRequest("anonymous", "realm1")]);

        // register a listener for the session becoming CONNECTED
        session.registerConnectedListener(

            // when the session becomes CONNECTED, trigger joining
            (connected) -> this.join(auth_requests)
        );

        // register a listener for the session becoming JOINED on a realm
        session.registerJoinedListener(

            // when the session becomes JOINED, log a message
            (joined) -> System.out.println("session joined: " + joined.to_string())
        );

        // register a listener for the session becoming READY
        session.registerReadyListener(

            // when the session becomes READY, log a message
            (ready) -> System.out.println("component ready: " + ready.to_string())
        );

        // register a listener for the session becoming DISCONNECTED
        session.registerDisconnectedListener(

            new DisconnectedListener() {

                @Override
                public void onDisconnected (Disconnected disconnected) {

                    if (disconnected.connection_success > 0 && disconnected.reconnection_attempts < 10) {
                        this.connect(disconnected.channel);
                    } else {
                        System.out.println("Giving up reconnecting!");
                    }
                }
            }
        );

        // now actually connect the session to the channel, which will then
        // trigger of a cascade of events and event handling code as above
        session.connect(channel);
    }
}
```

## Complete Example

The following is a sketch of a complete WAMP client using the low-level user API if the library. It creates a session and registers lifecycle event handlers. When the session becomes JOINED, a WAMP procedure is registered. When the underlying transport channel is lost, the channel is automatically reconnected.


```java
public class Main {

    public static void main (String[] args) {

        // creating a session can be done on the green field
        Session session = new Session();

        // then, define a sequence of authentication requests
        List<AuthRequest> auth_requests = new List([new AnonymousAuthRequest("anonymous", "realm1")]);

        // register a listener for the session becoming CONNECTED
        session.registerSessionConnectedListener(

            // when the session becomes CONNECTED, trigger joining
            (connected) -> this.join(auth_requests)
        );

        // register a listener for the session becoming JOINED
        session.registerJoinedListener(

            // when the session becomes JOINED, register a procedure ..
            (joined_details) -> session.register(
                "com.example.add2",
                (args, kwargs, details) -> return (args[0] + args[1], null, null)
            )
        );

        // register a listener for the session becoming READY
        session.registerReadyListener(

            // when the session becomes READY, log a message
            (ready) -> System.out.println("component ready: " + ready.to_string())
        );

        // register a listener for the session becoming DISCONNECTED
        session.registerSessionDisconnectedListener(

            new SessionDisconnectedListener() {

                @Override
                public void on (SessionDisconnected disconnected) {

                    if (disconnected.connection_success > 0 && disconnected.reconnection_attempts < 10) {
                        disconnected.channel.connect();
                    } else {
                        System.out.println("Giving up reconnecting!");
                    }
                }
            }
        );

        // serializers we want to run
        Serializer serializers = new List([new CborSerializer()]);

        // creating a WebSocket channel
        MessageChannel channel = new WebSocketChannel("wss://demo.crossbar.io", "realm1", serializers);

        // register a listener for the session becoming DISCONNECTED
        channel.registerChannelConnectedListener(

            new ChannelConnectedListener() {

                @Override
                public void on (ChannelConnected connected) {
                    // now actually connect the session to the channel, which will then
                    // trigger of cascade of events and event handling code as above
                    session.connect(this);
                }
            }
        );

        // now actually connect the channel, which will then trigger a cascade of events
        // and event handling code as above
        channel.connect();
    }
}
```
