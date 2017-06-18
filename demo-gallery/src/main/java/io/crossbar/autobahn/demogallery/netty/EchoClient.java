package io.crossbar.autobahn.demogallery.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.NettyTransport;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.auth.AnonymousAuth;


public class EchoClient {
    private Client mClient;
    private Session mSession;

    public EchoClient(ExecutorService executor, String uri, String realm) {

        // first, we create a session object (that may or may not be reused)
        mSession = new Session();

        // when the session joins a realm, run our code
        mSession.addOnJoinListener(details -> funStuff());

        // .. and we can have multiple listeners!
        mSession.addOnJoinListener(details -> funStuff2(details));

        // now create a transport list for the transport to try
        // and which will carry our session
        List<ITransport> transports = new ArrayList<>();

        // in this case, the only transport we add is a WAMP-over-WebSocket
        // implementation on top of Netty client WebSocket
        transports.add(new NettyTransport(uri));

        // now create a authenticator list for the session to announce
        // and which will authenticate our session
        List<IAuthenticator> authenticators = new ArrayList<>();

        // in this case, we don't care about authentication and so
        // the only authenticator we announce is the (pseudo) "anyonymous"
        authenticators.add(new AnonymousAuth());

        // finally, provide everything to a Client instance
        mClient = new Client(mSession, transports, realm, authenticators);
    }

    public void funStuff2 (SessionDetails details) {
        System.out.println("JOINED 2: sessionID=" + details.sessionID + " on realm=" + details.realm);

        // here we do an outoing remote call (WAMP RPC):
        List<Object> args = new ArrayList<>();
        args.add(2);
        args.add(3);

        CompletableFuture<CallResult> result =
            mSession.call("com.example.add2", args, null, null);

        result.thenAccept(callResult -> {
            System.out.println("got result: " + callResult.results.get(0));
        });
    }

    public void funStuff() {
        System.out.println("JOINED 1");
        // Here we do a subscribe to a topic.
        CompletableFuture<Subscription> subscriptionCompletableFuture = mSession.subscribe(
                "com.byteshaft.topic1", this::message, null);
        subscriptionCompletableFuture.thenAccept(subscription -> {
            System.out.println(subscription.topic);
        });

        // here we do an outoing remote call (WAMP RPC):
        CallOptions options = new CallOptions(5);
        CompletableFuture<CallResult> resultCompletableFuture = mSession.call(
                "com.byteshaft.grab_screenshot", null, null, options);
        resultCompletableFuture.thenAccept(callResult -> {
            System.out.println(callResult.results.get(0));
            System.out.println(callResult.kwresults);
        });

        // FIXME: add the error handling

//        mSession.subscribe("com.byteshaft.topic1", this::message, null);
//        mSession.register("com.byteshaft.exp", this::exp, null);
    }

    private CompletableFuture<InvocationResult> exp(List<Object> args, Map<String, Object> kwargs,
                                                    InvocationDetails details) {
        CompletableFuture<InvocationResult> future = new CompletableFuture<>();
        System.out.println("Called");
        return future;

    }

    private Void message(List<Object> args, Map<String, Object> kwargs) {
        System.out.println(args);
        System.out.println(kwargs);
        return null;
    }

    public void start() {
        CompletableFuture<ExitInfo> exitInfoCompletableFuture = mClient.connect();
        exitInfoCompletableFuture.thenApply(exitInfo -> mClient.connect());
    }
}
