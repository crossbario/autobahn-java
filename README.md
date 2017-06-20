# Autobahn|Java

## **Under Development**

Autobahn|Java is a subproject of the [Autobahn project](http://crossbar.io/autobahn/) and provides open-source client implementations for

* **[The WebSocket Protocol](http://tools.ietf.org/html/rfc6455)**
* **[The Web Application Messaging Protocol (WAMP)](http://wamp.ws/)**

running on Android and Netty/JVM.

The WebSocket layer is using a callback based user API, and is specifically written for Android.

The WAMP layer is using Java 8 **CompletableFuture** for WAMP actions (call, register, publish and subscribe) and the **Observer pattern** for WAMP session, subscription and registration lifecycle events.

The library is MIT licensed, maintained by the Crossbar.io Project, tested using the AutobahnTestsuite and published as a JAR to Maven and as a Docker toolchain image to Dockerhub.


## Get in touch

Get in touch on IRC #autobahn on chat.freenode.net or join the [mailing list](http://groups.google.com/group/autobahnws).


## Show me the code

### WebSocket on Android

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

### WAMP on Netty

#### Plumbing

Here is a top level program

```java
public class Main {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Service service = new Service(executor);

        int returnCode = service.start("ws://localhost:8080/ws", "realm1");

        System.exit(returnCode);
    }
}
```

that uses the following service to connect to a WAMP router like Crossbar.io and starting a WAMP session on a realm

```java
public class Service {

    private final ExecutorService mExecutor;

    // This is the central object to interact with Crossbar.io
    // a WAMP session runs over a transport, uses authenticators
    // and finally joins a realm.
    private final Session mSession;


    public Service(ExecutorService executor) {
        // everything should be run on the user supplied executor
        mExecutor = executor;

        // first, we create a session object (that may or may not be reused)
        mSession = new Session(executor);

        // when the session joins a realm, run our code
        // .. and we can have multiple listeners! there are other lifecycle
        // events to get notified for as well.
        mSession.addOnJoinListener(this::onJoinHandler1);
    }


    public int start(String url, String realm) {
        // now create a transport list for the transport to try
        // and which will carry our session
        List<ITransport> transports = new ArrayList<>();

        // in this case, the only transport we add is a WAMP-over-WebSocket
        // implementation on top of Netty client WebSocket
        transports.add(new NettyTransport(url));

        // now create a authenticator list for the session to announce
        // and which will authenticate our session
        List<IAuthenticator> authenticators = new ArrayList<>();

        // in this case, we don't care about authentication and so
        // the only authenticator we announce is the (pseudo) "anyonymous"
        authenticators.add(new AnonymousAuth());

        // finally, provide everything to a Client instance
        Client client = new Client(transports, mExecutor);

        // leave room for adding more than one sessions.
        client.add(mSession, realm, authenticators);

        CompletableFuture<ExitInfo> exitInfoCompletableFuture = client.connect();
        try {
            ExitInfo exitInfo = exitInfoCompletableFuture.get();
            return exitInfo.code;
        } catch (Exception e) {
            System.out.println(e);
            return 1;
        }
    }


    public void onJoinHandler1(SessionDetails details) {
        System.out.println("onJoinHandler1 fired: sessionid=" + details.sessionID + ", authid=" + details.authid + ", realm=" + details.realm);
    }
}
```

#### Using WAMP

After the plumbing has created a WAMP session, and when the session has joined a realm, the onJoin observers will get notified, and this is where usually topics are subscribed to or procedures are registered.

Here is how the four WAMP actions look like in AutobahnJava.

##### Call

##### Register

##### Publish

##### Subscribe

