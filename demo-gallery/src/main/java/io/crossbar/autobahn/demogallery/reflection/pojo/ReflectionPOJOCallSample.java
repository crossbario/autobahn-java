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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReflectionPOJOCallSample {

    public static CompletableFuture<ExitInfo> callReflectionPOJOResult(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {

            IPOJOServiceProxy proxy = session.getReflectionServices().getCalleeProxy(IPOJOServiceProxy.class);

            CompletableFuture<Person> personFuture = proxy.getPersonAsync();

            personFuture.whenComplete((person, throwable) -> {
                System.out.println(String.format("Person: %s %s in department %s",
                        person.firstname, person.lastname, person.department));
            });

            CompletableFuture<List<Person>> peopleFuture = proxy.getPeopleAsync();

            peopleFuture.whenComplete((persons, throwable) -> {
                System.out.println(String.format("Got %s persons", persons.size()));
                for (Person p: persons) {
                    System.out.println(String.format("Person: %s %s in department %s",
                            p.firstname, p.lastname, p.department));
                }
            });
        });
        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }
}
