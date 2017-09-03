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

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.AnonymousAuth;
import io.crossbar.autobahn.wamp.interfaces.IAuthenticator;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.transports.NettyTransport;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.ReceptionResult;
import io.crossbar.autobahn.wamp.types.RegisterOptions;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.Subscription;


public class Service {

    private static final Logger LOGGER = Logger.getLogger(Service.class.getName());

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
        //mSession.addOnJoinListener(this::onJoinHandler1);
        //mSession.addOnJoinListener(this::onJoinHandler2);
        //mSession.addOnJoinListener(this::onJoinHandler3);
        mSession.addOnJoinListener(this::onJoinHandler4);
    }


    public int start(String url, String realm) {
        LOGGER.info(String.format("Called with url=%s, realm=%s", url, realm));
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
            LOGGER.severe(e.getMessage());
            return 1;
        }
    }


    public void onJoinHandler1(Session session, SessionDetails details) {
        LOGGER.info(String.format(
                "onJoinHandler1 fired: sessionID=%s, authID=%s, realm=%s",
                details.sessionID, details.authid, details.realm));

        // Here we SUBSCRIBE to a topic
        CompletableFuture<Subscription> f1 = mSession.subscribe(
                "com.example.oncounter", this::onCounter, null);

        f1.thenAccept(subscription ->
                LOGGER.info(String.format("Subscribed to topic: %s", subscription.topic)));
        f1.exceptionally(throwable -> {
            LOGGER.info(String.format("ERROR - subscription failed: %s", throwable.getMessage()));
            return null;
        });

        // Here we REGISTER a procedure
        RegisterOptions options = new RegisterOptions(
                RegisterOptions.MATCH_EXACT, RegisterOptions.INVOKE_ROUNDROBIN);
        CompletableFuture<Registration> f2 = mSession.register(
                "com.example.add2", this::add2, options);

        f2.thenAccept(registration ->
                LOGGER.info(String.format("Registered procedure: %s", registration.toString())));

        f2.exceptionally(throwable -> {
            LOGGER.info(String.format("ERROR - registration failed: %s", throwable.getMessage()));
            return null;
        });
    }


    public void onJoinHandler2(Session session, SessionDetails details) {
        LOGGER.info("onJoinHandler2 fired");

        List<Object> args = new ArrayList<>();
        args.add(2);
        args.add(3);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {

            // here we CALL every second
            CompletableFuture<CallResult> f =
                mSession.call("com.example.add2", args, null, null);

            f.thenAccept(result ->
                    LOGGER.info(String.format("Got result: %s, ", result.results.get(0))));

            f.exceptionally(throwable -> {
                LOGGER.info(String.format("ERROR - call failed: %s", throwable.getMessage()));
                return null;
            });

        }, 0, 1, TimeUnit.SECONDS);
    }


    public void onJoinHandler3(Session session, SessionDetails details) {
        LOGGER.info("onJoinHandler3 fired");

        PublishOptions options = new PublishOptions(true, false);

        List<Object> argsCounter = new ArrayList<>();
        argsCounter.add(details.sessionID);
        argsCounter.add("Java");

        final int[] i = new int[1];

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            argsCounter.add(0, i[0]);

            // here we PUBLISH every second
            CompletableFuture<Publication> f = mSession.publish(
                    "com.example.oncounter", argsCounter, null, options);

            f.thenAccept(publication ->
                    LOGGER.info(String.format("event published: %s", publication.publication))
            );

            f.exceptionally(throwable -> {
                LOGGER.info(String.format("ERROR - publication failed: %s", throwable.getMessage()));
                return null;
            });

            argsCounter.remove(0);
            i[0] += 1;
        }, 0, 1, TimeUnit.SECONDS);
    }


    public void onJoinHandler4(Session session, SessionDetails details) {
        LOGGER.info("onJoinHandler4 fired");

        // call a remote procedure that returns a Person
        CompletableFuture<Void> f1 =
            mSession.call("com.example.get_person", null, null, new TypeReference<Person>() {}, null)
                .handleAsync(
                    (person, throwable) -> {
                        if (throwable != null) {
                            LOGGER.info(String.format("get_person() ERROR: %s", throwable.getMessage()));
                        } else {
                            LOGGER.info(String.format(
                                    "get_person() [typed]: %s %s (%s)",
                                    person.firstname, person.lastname, person.department));
                        }
                        return null;
                    }, mExecutor
                )
        ;

        // call a remote procedure that returns a Person .. slowly (3 secs delay)
        CompletableFuture<Void> f2 =
            mSession.call("com.example.get_person_delayed", null, null, new TypeReference<Person>() {}, null)
                .handleAsync(
                    (person, throwable) -> {
                        if (throwable != null) {
                            LOGGER.info(String.format("get_person_delayed() ERROR: %s",
                                    throwable.getMessage()));
                        } else {
                            LOGGER.info(String.format(
                                    "get_person_delayed() [typed]: %s %s (%s)",
                                    person.firstname, person.lastname, person.department));
                        }
                        return null;
                    }, mExecutor
                )
        ;

        // call a remote procedure that returns a List<Person>
        CompletableFuture<Void> f3 =
            mSession.call("com.example.get_all_persons", null, null, new TypeReference<List<Person>>() {}, null)
                .handleAsync(
                    (persons, throwable) -> {
                        if (throwable != null) {
                            LOGGER.info(String.format(
                                    "get_all_persons() ERROR: %s", throwable.getMessage()));
                        } else {
                            LOGGER.info("get_all_persons() [typed]:");
                            for (Person person: persons) {
                                LOGGER.info(String.format("%s %s (%s)",
                                        person.firstname, person.lastname, person.department));
                            }
                        }
                        return null;
                    }, mExecutor
                )
        ;

        // call a remote procedure that returns a List<Person>
        List<Object> args = new ArrayList<>();
        args.add("development");

        CompletableFuture<Void> f4 =
            mSession.call("com.example.get_persons_by_department", args, null, new TypeReference<List<Person>>() {}, null)
                .handleAsync(
                    (persons, throwable) -> {
                        if (throwable != null) {
                            LOGGER.info(String.format(
                                    "get_persons_by_department() ERROR: %s",
                                    throwable.getMessage()));
                        } else {
                            LOGGER.info("get_persons_by_department() [typed]:");
                            for (Person person: persons) {
                                LOGGER.info(String.format("%s %s (%s)",
                                        person.firstname, person.lastname, person.department));
                            }
                        }
                        return null;
                    }, mExecutor
                )
        ;

        // call a remote procedure that returns a Map<String, List<Person>>
        CompletableFuture<Void> f5 =
            mSession.call("com.example.get_persons_by_department", null, null, new TypeReference<Map<String, List<Person>>>() {}, null)
                .handleAsync(
                    (Map<String, List<Person>> persons_by_department, Throwable throwable) -> {
                        if (throwable != null) {
                            LOGGER.info(String.format(
                                    "get_persons_by_department() ERROR: %s", throwable.getMessage()));
                        } else {

                            LOGGER.info("get_persons_by_department() [typed]:");
                            for (String department: persons_by_department.keySet()) {
                                LOGGER.info(String.format("department '%s:'", department));
                                List<Person> persons = persons_by_department.get(department);
                                for (Person person: persons) {
                                    LOGGER.info(String.format("%s %s", person.firstname, person.lastname));
                                }
                            }
                        }
                        return null;
                    }, mExecutor
                )
        ;

        CompletableFuture.allOf(f1, f2, f3, f4, f5)
            .thenRunAsync(
                () -> {
                    LOGGER.info("all done!");
                    mSession.leave("wamp.close.normal", "all done!");
                }, mExecutor
            )
        ;
    }


    // this procedure is registered and can be called remotely
    private CompletableFuture<InvocationResult> add2(List<Object> args, Map<String, Object> kwargs,
                                                     InvocationDetails details) {
        int res = (int) args.get(0) + (int) args.get(1);
        List<Object> arr = new ArrayList<>();
        arr.add(res);
        arr.add("Java");
        return CompletableFuture.completedFuture(new InvocationResult(arr));
    }


    // this handler will process incoming events for the topic we subscribe it to
    private void onCounter(List<Object> args,
                           Map<String, Object> kwargs,
                           EventDetails details) {
        LOGGER.info(String.format("received counter: %s", args.get(0)));
    }

    private CompletableFuture<ReceptionResult> onCounter1(List<Object> args,
                                                          Map<String, Object> kwargs,
                                                          EventDetails details) {
        LOGGER.info(String.format("received counter: %s", args.get(0)));
        return CompletableFuture.completedFuture(new ReceptionResult());
    }

    private void onCounterSimple(String object, EventDetails details) {
        LOGGER.info(String.format("received counter: %s", object));
    }

    static class Person {
        public String firstname;
        public String lastname;
        public String department;

        public Person() {
            this.firstname = "unknown";
            this.lastname = "unknown";
            this.department = "unknown";
        }

        public Person(String firstname, String lastname, String department) {
            this.firstname = firstname;
            this.lastname = lastname;
            this.department = department;
        }
    }
}
