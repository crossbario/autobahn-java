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

package io.crossbar.autobahn.demogallery.reflection.pojo;

import io.crossbar.autobahn.demogallery.data.Person;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReflectionPOJORegisterSample {
    public static CompletableFuture<ExitInfo> registerReflectionPOJO(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {

            final IPOJOService service = new IPOJOService() {
                @Override
                public Person getPerson() {
                    return new Person("john", "doe", "hr");
                }

                @Override
                public List<Person> getPeople() {
                    List<Person> persons = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        persons.add(new Person("john", "doe", "hr"));
                    }
                    return persons;
                }
            };

            List<CompletableFuture<Registration>> registrations =
                    session.getReflectionServices()
                    .registerCallee(service);

            for (CompletableFuture<Registration> registrationCompletableFuture : registrations) {
                registrationCompletableFuture.whenComplete((registration, throwable) -> {
                    System.out.println(String.format(
                            "Registered procedure %s", registration.procedure));
                    });
            }
        });

        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }
}
