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
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import io.crossbar.autobahn.demogallery.data.Person;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.SessionDetails;


public class Service {

    private static final Logger LOGGER = Logger.getLogger(Service.class.getName());

    private final Executor mExecutor;
    // This is the central object to interact with Crossbar.io
    // a WAMP session runs over a transport, uses authenticators
    // and finally joins a realm.
    private final Session mSession;

    public Service(Executor executor) {
        // everything should be run on the user supplied executor
        mExecutor = executor;

        // first, we create a session object (that may or may not be reused)
        mSession = new Session(executor);

        // when the session joins a realm, run our code
        mSession.addOnJoinListener(this::onJoinHandler4);
    }

    public int start(String url, String realm) {
        LOGGER.info(String.format("Called with url=%s, realm=%s", url, realm));
        // finally, provide everything to a Client instance
        Client client = new Client(mSession, url, realm, mExecutor);
        CompletableFuture<ExitInfo> exitFuture = client.connect();
        try {
            ExitInfo exitInfo = exitFuture.get();
            return exitInfo.code;
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            return 1;
        }
    }

    private void onJoinHandler4(Session session, SessionDetails details) {
        LOGGER.info("onJoinHandler4 fired");

        // call a remote procedure that returns a Person
        CompletableFuture<Person> f1 = mSession.call("com.example.get_person", Person.class);
        f1.whenCompleteAsync((person, throwable) -> {
            if (throwable != null) {
                LOGGER.info(String.format("get_person() ERROR: %s", throwable.getMessage()));
                System.exit(1);
            } else {
                LOGGER.info(String.format("get_person() [typed]: %s %s (%s)", person.firstname,
                        person.lastname, person.department));
            }
        }, mExecutor);

        // call a remote procedure that returns a Person .. slowly (3 secs delay)
        CompletableFuture<Person> f2 = mSession.call("com.example.get_person_delayed",
                Person.class);
        f2.whenCompleteAsync((person, throwable) -> {
            if (throwable != null) {
                LOGGER.info(String.format("get_person_delayed() ERROR: %s",
                        throwable.getMessage()));
                System.exit(1);
            } else {
                LOGGER.info(String.format("get_person_delayed() [typed]: %s %s (%s)",
                        person.firstname, person.lastname, person.department));
            }
        }, mExecutor);

        // call a remote procedure that returns a List<Person>
        CompletableFuture<List<Person>> f3 = mSession.call("com.example.get_all_persons",
                new TypeReference<List<Person>>() {});
        f3.whenCompleteAsync((persons, throwable) -> {
            if (throwable != null) {
                LOGGER.info(String.format("get_all_persons() ERROR: %s", throwable.getMessage()));
                System.exit(1);
            } else {
                LOGGER.info("get_all_persons() [typed]:");
                for (Person person: persons) {
                    LOGGER.info(String.format("%s %s (%s)", person.firstname, person.lastname,
                            person.department));
                }
            }
        }, mExecutor);

        // call a remote procedure that returns a List<Person>
        List<Object> args = new ArrayList<>();
        args.add("development");

        CompletableFuture<List<Person>> f4 = mSession.call("com.example.get_persons_by_department",
                args, new TypeReference<List<Person>>() {});
        f4.whenCompleteAsync((persons, throwable) -> {
            if (throwable != null) {
                LOGGER.info(String.format("get_persons_by_department() ERROR: %s",
                        throwable.getMessage()));
                System.exit(1);
            } else {
                LOGGER.info("get_persons_by_department() [typed]:");
                for (Person person: persons) {
                    LOGGER.info(String.format("%s %s (%s)", person.firstname, person.lastname,
                            person.department));
                }
            }
        }, mExecutor);

        // call a remote procedure that returns a Map<String, List<Person>>
        CompletableFuture<Map<String, List<Person>>> f5 = mSession.call(
                "com.example.get_persons_by_department",
                new TypeReference<Map<String, List<Person>>>() {});
        f5.whenCompleteAsync((persons_by_department, throwable) -> {
            if (throwable != null) {
                LOGGER.info(String.format("get_persons_by_department() ERROR: %s",
                        throwable.getMessage()));
                System.exit(1);
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
        });

        CompletableFuture.allOf(f1, f2, f3, f4, f5).thenRunAsync(() -> {
            LOGGER.info("all done!");
            mSession.leave("wamp.close.normal", "all done!");
        }, mExecutor);
    }
}
