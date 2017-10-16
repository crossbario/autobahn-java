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

package io.crossbar.autobahn.demogallery;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.Subscription;

public class ExampleClient {

    private static final Logger LOGGER = Logger.getLogger(ExampleClient.class.getName());
    private static final String PROC_ADD2 = "com.example.add2";
    private static final String TOPIC_COUNTER = "com.example.oncounter";

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
        CompletableFuture<Registration> regFuture = session.register(PROC_ADD2, this::add2);
        regFuture.thenAccept(reg -> LOGGER.info("Registered procedure: com.example.add2"));

        CompletableFuture<Subscription> subFuture = session.subscribe(
                TOPIC_COUNTER, this::onCounter);
        subFuture.thenAccept(subscription ->
                LOGGER.info(String.format("Subscribed to topic: %s", subscription.topic)));

        final int[] x = {0};
        final int[] counter = {0};

        final PublishOptions publishOptions = new PublishOptions(true, false);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {

            // here we CALL every second
            CompletableFuture<CallResult> f = session.call(PROC_ADD2, x[0], 3);
            f.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    LOGGER.info(String.format("Got result: %s, ", callResult.results.get(0)));
                    x[0] += 1;
                } else {
                    LOGGER.info(String.format("ERROR - call failed: %s", throwable.getMessage()));
                }
            });

            CompletableFuture<Publication> p = session.publish(
                    TOPIC_COUNTER, publishOptions, counter[0], session.getID(), "Java");
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

    private List<Object> add2(List<Integer> args, InvocationDetails details) {
        int res = args.get(0) + args.get(1);
        return Arrays.asList(res, details.session.getID(), "Java");
    }

    private void onCounter(List<Object> args) {
        LOGGER.info(String.format("oncounter event, counter value=%s from component %s (%s)",
                args.get(0), args.get(1), args.get(2)));
    }
}
