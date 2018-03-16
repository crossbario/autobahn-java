package io.crossbar.autobahn.demogallery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.demogallery.data.Person;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.Registration;

public class POJORegisterExamples {
    public static CompletableFuture<ExitInfo> registerSimplePOJO(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<Registration> regFuture = session.register(
                    "io.crossbar.example.get_person", POJORegisterExamples::get_person);
            regFuture.whenComplete((registration, throwable) -> {
                System.out.println(String.format(
                        "Registered procedure %s", registration.procedure));
            });
        });
        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }

    public static CompletableFuture<ExitInfo> registerListPOJOs(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<Registration> regFuture = session.register(
                    "io.crossbar.example.get_person", POJORegisterExamples::get_persons);
            regFuture.whenComplete((registration, throwable) -> {
                System.out.println(String.format(
                        "Registered procedure %s", registration.procedure));
            });
        });
        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }

    private static Person get_person() {
        return new Person("john", "doe", "hr");
    }

    private static List<Person> get_persons() {
        List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            persons.add(new Person("john", "doe", "hr"));
        }
        return persons;
    }
}
