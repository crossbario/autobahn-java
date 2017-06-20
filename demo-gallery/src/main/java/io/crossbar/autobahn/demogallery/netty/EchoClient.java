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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.NettyTransport;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.AnonymousAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.Subscription;


public class EchoClient {
    private Client mClient;
    private Session mSession;

    public EchoClient(ExecutorService executor, String uri, String realm) {

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

        // here we do an outoing remote call (WAMP RPC):
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

        CompletableFuture<Subscription> counterRes = mSession.subscribe(
                "com.example.oncounter", this::onCounter, null);

        counterRes.thenAccept(subscription -> System.out.println("subscribed to topic: " + subscription.topic));

        PublishOptions options = new PublishOptions(true, true);
        List<Object> argsCounter = new ArrayList<>();
        argsCounter.add(details.sessionID);
        argsCounter.add("Java");
        int i = 1;
        while (true) {
            argsCounter.add(0, i);
            CompletableFuture<Publication> pubFuture = mSession.publish(
                    "com.example.oncounter", argsCounter, null, options);
            pubFuture.thenAccept(publication -> System.out.println("published: " + publication.publication));
            try {
                pubFuture.get();
                Thread.sleep(1000);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            argsCounter.remove(0);
            i += 1;
        }
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
