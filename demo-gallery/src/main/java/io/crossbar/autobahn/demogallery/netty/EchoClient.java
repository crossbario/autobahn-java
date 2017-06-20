///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.demogallery.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.NettyTransport;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.AnonymousAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.Subscription;


public class EchoClient {
    private Client mClient;
    private Session mSession;
    private ExecutorService mExecutor;

    public EchoClient(ExecutorService executor, String uri, String realm) {

        mExecutor = executor;

        // first, we create a session object (that may or may not be reused)
        mSession = new Session(executor);

        // when the session joins a realm, run our code
        //mSession.addOnJoinListener(details -> funStuff());

        // .. and we can have multiple listeners!
        mSession.addOnJoinListener(this::onJoinHandler);

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
        mClient = new Client(transports, executor);

        // leave room for adding more than one sessions.
        mClient.add(mSession, realm, authenticators);

    }

    public void onJoinHandler(SessionDetails details) {
        System.out.println("JOINED 2: sessionID=" + details.sessionID + " on realm=" + details.realm);

        // here we do an outgoing remote call (WAMP RPC):
        List<Object> args = new ArrayList<>();
        args.add(2);
        args.add(3);

        CompletableFuture<CallResult> result =
            mSession.call("com.example.add2", args, null, null);

        result.thenAccept(callResult -> {
            System.out.println("got result: " + callResult.results.get(0));
//            mSession.leave("wamp.leave.normal", "sessio leaving realm normally.");
        });

        result.exceptionally(throwable -> {
            System.out.println(throwable.getMessage());
            return null;
        });

        // Here we subscribe to a remote procedure.
        CompletableFuture<Subscription> counterRes = mSession.subscribe(
                "com.example.oncounter", this::onCounter, null);

        counterRes.thenAccept(subscription -> System.out.println("subscribed to topic: " + subscription.topic));

        // Here we register a remote procedure.
        CompletableFuture<Registration> regFuture = mSession.register(
                "com.example.add", this::add2, null);
        regFuture.thenAccept(registration -> System.out.println("Registered procedure: " + registration.procedure));

        // Here we publish an event.
        PublishOptions options = new PublishOptions(true, true);
        List<Object> argsCounter = new ArrayList<>();
        argsCounter.add(details.sessionID);
        argsCounter.add("Java");
        final int[] i = new int[1];
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            argsCounter.add(0, i[0]);
            CompletableFuture<Publication> pubFuture = mSession.publish(
                    "com.example.oncounter", argsCounter, null, options);
            pubFuture.thenAccept(publication -> System.out.println("published: " + publication.publication));
            argsCounter.remove(0);
            i[0] += 1;
        }, 0, 1, TimeUnit.SECONDS);
    }

    private CompletableFuture<InvocationResult> add2(List<Object> args, Map<String, Object> kwargs,
                                                     InvocationDetails details) {
        CompletableFuture<InvocationResult> future = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            int res = (int) args.get(0) + (int) args.get(1);
            List<Object> arr = new ArrayList<>();
            arr.add(res);
            arr.add("Netty");
            return new InvocationResult(arr);
        }, mExecutor).thenApplyAsync(future::complete, mExecutor);
        System.out.println("CALLED: " + args);
        return future;
    }

    private Void onCounter(List<Object> args, Map<String, Object> kwargs) {
        System.out.println("got counter: " + args.get(0));
        return null;
    }

    public int start() {
        CompletableFuture<ExitInfo> exitInfoCompletableFuture = mClient.connect();
        try {
            ExitInfo exitInfo = exitInfoCompletableFuture.get();
            return exitInfo.code;
        } catch (Exception e) {
            System.out.println(e);
            return 1;
        }
    }
}
