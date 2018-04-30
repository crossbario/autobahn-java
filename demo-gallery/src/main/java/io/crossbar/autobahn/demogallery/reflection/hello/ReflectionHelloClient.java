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

package io.crossbar.autobahn.demogallery.reflection.hello;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ReflectionHelloClient {

    private static final Logger LOGGER = Logger.getLogger(ReflectionHelloClient.class.getName());

    public CompletableFuture<ExitInfo> main(String websocketURL, String realm) {
        Session session = new Session();
        session.addOnConnectListener(this::onConnectCallback);
        session.addOnJoinListener(this::onJoinCallback);
        session.addOnLeaveListener(this::onLeaveCallback);
        session.addOnDisconnectListener(this::onDisconnectCallback);

        // finally, provide everything to a Client instance and connect
        Client client = new Client(session, websocketURL, realm);
        return client.connect();
    }

    private void onConnectCallback(Session session) {
        LOGGER.info("Session connected, ID=" + session.getID());
    }

    private void onJoinCallback(Session session, SessionDetails details) {
        IAdd2Service add2Service = new IAdd2Service() {
            @Override
            public int add2(int x, int y) {
                return (x+y);
            }
        };

        ICounterSubscriber subscriber = new ICounterSubscriber() {
            @Override
            public void onCounter(int counter) {
                LOGGER.info(String.format("oncounter event, counter value=%s",
                        counter));
            }
        };

        List<CompletableFuture<Registration>> regFuture = session.getReflectionServices().registerCallee(add2Service);
        List<CompletableFuture<Subscription>> subFuture = session.getReflectionServices().registerSubscriber(subscriber);

        regFuture.get(0).thenAccept(reg -> LOGGER.info("Registered procedure: com.example.add2"));

        subFuture.get(0).thenAccept(subscription ->
                LOGGER.info(String.format("Subscribed to topic: %s", subscription.topic)));

        final int[] x = {0};
        final int[] counter = {0};

        final PublishOptions publishOptions = new PublishOptions(true, false);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {

            IAdd2ServiceProxy proxy =
                    session.getReflectionServices().getCalleeProxy(IAdd2ServiceProxy.class);

            // here we CALL every second
            CompletableFuture<Integer> f = proxy.add2Async(x[0],3);
            f.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    LOGGER.info(String.format("Got result: %s, ", result));
                    x[0] += 1;
                } else {
                    LOGGER.info(String.format("ERROR - call failed: %s", throwable.getMessage()));
                }
            });

            final String onCounterUri = "com.example.oncounter";

            CompletableFuture<Publication> p = session.publish(
                    onCounterUri, publishOptions, counter[0], session.getID(), "Java");
            p.whenComplete((publication, throwable) -> {
                if (throwable == null) {
                    LOGGER.info("published to 'oncounter' with counter " + counter[0]);
                    counter[0] += 1;
                } else {
                    LOGGER.info(String.format("ERROR - pub failed: %s", throwable.getMessage()));
                }
            });

        }, 0, 2, TimeUnit.SECONDS);
    }

    private void onLeaveCallback(Session session, CloseDetails detail) {
        LOGGER.info(String.format("Left reason=%s, message=%s", detail.reason, detail.message));
    }

    private void onDisconnectCallback(Session session, boolean wasClean) {
        LOGGER.info(String.format("Session with ID=%s, disconnected.", session.getID()));
    }
}