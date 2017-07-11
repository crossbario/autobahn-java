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
import io.netty.util.concurrent.CompleteFuture;


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
        //mSession.addOnJoinListener(this::onJoinHandler1);
        //mSession.addOnJoinListener(this::onJoinHandler2);
        //mSession.addOnJoinListener(this::onJoinHandler3);
        mSession.addOnJoinListener(this::onJoinHandler4);
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


    public void onJoinHandler1(Session session, SessionDetails details) {
        System.out.println("onJoinHandler1 fired: sessionid=" + details.sessionID + ", authid=" + details.authid + ", realm=" + details.realm);

        // Here we SUBSCRIBE to a topic
        CompletableFuture<Subscription> f1 = mSession.subscribe(
                "com.example.oncounter", this::onCounter, null);

        f1.thenAccept(subscription ->
            System.out.println("Subscribed to topic: " + subscription.topic)
        );
        f1.exceptionally(throwable -> {
            System.out.println("ERROR - subscription failed: " + throwable.getMessage());
            return null;
        });

        // Here we REGISTER a procedure
        RegisterOptions options = new RegisterOptions(RegisterOptions.MATCH_EXACT, RegisterOptions.INVOKE_ROUNDROBIN);
        CompletableFuture<Registration> f2 = mSession.register(
                "com.example.add2", this::add2, options);
        f2.thenAccept(registration ->
            System.out.println("Registered procedure: " + registration.procedure)
        );
        f2.exceptionally(throwable -> {
            System.out.println("ERROR - registration failed: " + throwable.getMessage());
            return null;
        });
    }


    public void onJoinHandler2(Session session, SessionDetails details) {
        System.out.println("onJoinHandler2 fired");

        List<Object> args = new ArrayList<>();
        args.add(2);
        args.add(3);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {

            // here we CALL every second
            CompletableFuture<CallResult> f =
                mSession.call("com.example.add2", args, null, null);

            f.thenAccept(result -> {
                System.out.println("got result: " + result.results.get(0));
            });
            f.exceptionally(throwable -> {
                System.out.println("ERROR - call failed: " + throwable.getMessage());
                return null;
            });

        }, 0, 1, TimeUnit.SECONDS);
    }


    public void onJoinHandler3(Session session, SessionDetails details) {
        System.out.println("onJoinHandler3 fired");

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
                System.out.println("event published: " + publication.publication)
            );
            f.exceptionally(throwable -> {
                System.out.println("ERROR - publication failed: " + throwable.getMessage());
                return null;
            });

            argsCounter.remove(0);
            i[0] += 1;
        }, 0, 1, TimeUnit.SECONDS);
    }


    public void onJoinHandler4(Session session, SessionDetails details) {
        System.out.println("onJoinHandler4 fired");

        // call a remote procedure that returns a Person
        CompletableFuture<Void> f1 =
            mSession.call("com.example.get_person", null, null, new TypeReference<Person>() {}, null)
                .handleAsync(
                    (person, throwable) -> {
                        if (throwable != null) {
                            System.out.println("get_person() ERROR: " + throwable.getMessage());
                            //throwable.printStackTrace();
                        } else {
                            System.out.println("get_person() [typed]: " + person.firstname + " " + person.lastname + " (" + person.department + ")");
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
                            System.out.println("get_person_delayed() ERROR: " + throwable.getMessage());
                            //throwable.printStackTrace();
                        } else {
                            System.out.println("get_person_delayed() [typed]: " + person.firstname + " " + person.lastname + " (" + person.department + ")");
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
                            System.out.println("get_all_persons() ERROR: " + throwable.getMessage());
                            //throwable.printStackTrace();
                        } else {
                            System.out.println("get_all_persons() [typed]:");
                            persons.forEach(person -> {
                                System.out.println(person.firstname + " " + person.lastname + " (" + person.department + ")");
                            });
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
                            System.out.println("get_persons_by_department() ERROR: " + throwable.getMessage());
                            //throwable.printStackTrace();
                        } else {
                            System.out.println("get_persons_by_department() [typed]:");
                            persons.forEach(person -> {
                                System.out.println(person.firstname + " " + person.lastname + " (" + person.department + ")");
                            });
                        }
                        return null;
                    }, mExecutor
                )
        ;

        // call a remote procedure that returns a Map<String, List<Person>>
        CompletableFuture<Void> f5 =
            mSession.call("com.example.get_persons_by_department", null, null, new TypeReference<Map<String, List<Person>>>() {}, null)
                .handleAsync(
                    (persons_by_department, throwable) -> {
                        if (throwable != null) {
                            System.out.println("get_persons_by_department() ERROR: " + throwable.getMessage());
                            //throwable.printStackTrace();
                        } else {

                            System.out.println("get_persons_by_department() [typed]:");

                            persons_by_department.forEach((department, persons) -> {
                                System.out.println("\ndepartment '" + department + "':");

                                persons.forEach(person -> {
                                    System.out.println("     " + person.firstname + " " + person.lastname);
                                });
                            });
                        }
                        return null;
                    }, mExecutor
                )
        ;

        CompletableFuture.allOf(f1, f2, f3, f4, f5)
            .thenRunAsync(
                () -> {
                    System.out.println("all done!");
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
    private CompletableFuture<ReceptionResult> onCounter(List<Object> args,
                                                      Map<String, Object> kwargs,
                                                      EventDetails details) {
        System.out.println("received counter: " + args.get(0));
        CompletableFuture<ReceptionResult> future = new CompletableFuture<>();
        return future;
    }

    private void onCounterSimple(String object, EventDetails details) {
        System.out.println("received counter: " + object);
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
