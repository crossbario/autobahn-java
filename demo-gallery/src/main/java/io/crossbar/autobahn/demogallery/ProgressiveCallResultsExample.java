package io.crossbar.autobahn.demogallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;
import io.crossbar.autobahn.wamp.types.Registration;

public class ProgressiveCallResultsExample {

    public static CompletableFuture<ExitInfo> registerProgressive(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<Registration> regFuture = session.register(
                    "io.crossbar.longop",
                    (List<Object> args, Map<String, Object> kwargs, InvocationDetails invocationDetails) -> {
                        for (int i = 0; i < 5; i++) {
                            List<Object> argsList = new ArrayList<>();
                            argsList.add(i);
                            invocationDetails.progress.sendProgress(argsList, null);
                        }
                        List<Object> resultArgs = new ArrayList<>();
                        resultArgs.add(7);
                        return CompletableFuture.completedFuture(new InvocationResult(resultArgs));
                    });

            regFuture.whenComplete((registration, throwable) -> {
                System.out.println(String.format(
                        "Registered procedure %s", registration.procedure));
            });
        });

        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }


    public static CompletableFuture<ExitInfo> callProgressive(String wsAddress, String realm) {
        Session wampSession = new Session();
        wampSession.addOnJoinListener((session, details) -> {
            CompletableFuture<CallResult> callFuture = session.call(
                    "io.crossbar.longop",
                    new CallOptions(result -> System.out.println("Receive Progress: " + result.results)));

            callFuture.whenComplete((callResult, throwable) -> {
                System.out.println(String.format("Call result: %s", callResult.results));
            });
        });

        Client wampClient = new Client(wampSession, wsAddress, realm);
        return wampClient.connect();
    }
}
