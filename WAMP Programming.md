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
                "Session connected to WAMP message channel using protocol " +
                channel.protocol.getName() + "'")
        );

        // assume "channel" is a WAMP channel object created magically
        // and connect the session to the channel
        session.connect(channel);

        // explicitly unregister the listener
        session.unregisterConnectedListener(listener);

        // now disconnect the session from the current channel (because otherwise,
        // we can't connect it to a different one - see below)
        session.disconnect();

        // now connect the session to a different channel. this will NOT fire
        // our listener as we have explicitly unregister that listener
        session.connect(channel2);
    }
}
```
