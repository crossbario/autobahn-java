package io.crossbar.autobahn.demogallery.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.Subscription;


public class CIService {
    private static final IABLogger LOGGER = ABLogger.getLogger(Service.class.getName());

    private final Executor mExecutor;
    // This is the central object to interact with Crossbar.io
    // a WAMP session runs over a transport, uses authenticators
    // and finally joins a realm.
    private final Session mSession;

    private boolean mProduce = true;

    public CIService(Executor executor) {
        // everything should be run on the user supplied executor
        mExecutor = executor;

        // first, we create a session object (that may or may not be reused)
        mSession = new Session(executor);

        // when the session joins a realm, run our code
        mSession.addOnJoinListener(this::consumer);
    }

    public int start(String url, String realm) {
        LOGGER.i(String.format("Called with url=%s, realm=%s", url, realm));
        // finally, provide everything to a Client instance
        Client client = new Client(mSession, url, realm, mExecutor);
        CompletableFuture<ExitInfo> exitFuture = client.connect();
        try {
            ExitInfo exitInfo = exitFuture.get();
            return exitInfo.code;
        } catch (Exception e) {
            LOGGER.e(e.getMessage());
            return 1;
        }
    }

    private void consumer(Session session, SessionDetails details) {

        final int[] counter = {0};
        CompletableFuture<Subscription> subFuture = session.subscribe(
                "io.crossbar.example.client1.oncounter",
                obj -> {
                    System.out.println(String.format("'oncounter' event, counter value: %s", obj));
                    counter[0] += 1;
                });
        subFuture.whenComplete((subscription, throwable) -> {
            if (throwable == null) {
                System.out.println("----------------------------");
                System.out.println("subscribed to topic 'io.crossbar.example.client1.oncounter'");
            }
        });

        final int[] x = {0};
        while (x[0] < 5 && counter[0] < 5) {
            CompletableFuture<CallResult> callFuture = session.call(
                    "io.crossbar.example.client1.add2", x[0], 3);
            callFuture.whenComplete((callResult, throwable) -> {
                if (throwable == null) {
                    LOGGER.i(String.format("Got result: %s, ", callResult.results.get(0)));
                    x[0] += 1;
                } else {
                    LOGGER.i(String.format("ERROR - call failed: %s", throwable.getMessage()));
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            session.call("io.crossbar.example.client1.stop_producing").get();
            producer(session);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private Object stopProducing(Object args) {
        mProduce = false;
        return 1;
    }

    private void producer(Session session) {
        RegisterOptions options = new RegisterOptions(null, "roundrobin");
        CompletableFuture<Registration> regFuture = session.register(
                "io.crossbar.example.client2.stop_producing", this::stopProducing, options);
        regFuture.whenComplete((registration, throwable) -> {
            if (throwable == null) {
                System.out.println("----------------------------");
                System.out.println("procedure registered: io.crossbar.example.client2.add2");
            }
        });

        final int[] counter = {0};
        final PublishOptions publishOptions = new PublishOptions(true, true);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            CompletableFuture<Publication> pubFuture = session.publish(
                    "io.crossbar.example.client2.oncounter", publishOptions, counter[0]);
            pubFuture.whenComplete((publication, throwable) -> {
                if (throwable == null) {
                    LOGGER.i("published to 'oncounter' with counter " + counter[0]);
                    counter[0] += 1;
                } else {
                    LOGGER.i(String.format("ERROR - pub failed: %s", throwable.getMessage()));
                }
            });

            if (!mProduce) {
                session.leave();
                executorService.shutdown();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}
