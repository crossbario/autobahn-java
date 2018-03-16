package io.crossbar.autobahn.demogallery;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.demogallery.data.Person;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.ExitInfo;

public class POJOCallExamples {

    public static CompletableFuture<ExitInfo> callResultSimplePOJO(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<Person> callFuture = session.call(
                    "com.example.get_person", Person.class);

            callFuture.whenComplete((person, throwable) -> {
                System.out.println(String.format("Person: %s %s in department %s",
                        person.firstname, person.lastname, person.department));
            });
        });
        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }

    public static CompletableFuture<ExitInfo> callResultListPOJOs(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<List<Person>> callFuture = session.call(
                    "com.example.get_all_persons", new TypeReference<List<Person>>() {});

            callFuture.whenComplete((persons, throwable) -> {
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
