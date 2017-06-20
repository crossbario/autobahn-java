package msv;

import java.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import io.crossbar.autobahn.wamp.interfaces.*;
import io.crossbar.autobahn.wamp.types.*;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.Client;

import io.crossbar.autobahn.wamp.transport.NettyWebSocketTransport;
import io.crossbar.autobahn.wamp.auth.AnonymousAuth;


public class Main {

    private static void run(Session session, SessionDetails details) {

        int errors = 0;
        int successes = 0;

        int a = 23;
        int b = 666;

        List<Object> args = new ArrayList<>();
        args.add(a);
        args.add(b);

        CompletableFuture<CallResult> result =
            session.call("com.example.add2", args, null, null);

        result.thenAccept(callResult -> {
            int result = callResult.results.get(0);
            if (result == a + b) {
                System.out.println("ok, got expected result: " + result);
                successes += 1;
                return 0;
            } else {
                System.out.println("error! got wrong result: " + result);
                errors += 1;
                return 1;
            }
        });

        result.exceptionally(throwable -> {
            System.out.println("error! got no result at all: " + throwable.getMessage());
            errors += 1;
            return 1;
        });

        result.thenApply((err) -> {
            if (err > 0) {
                session.leave("wamp.leave.error", "errors happened! " + errors + " and " + successes + " in sum.");
            } else {
                session.leave("wamp.leave.normal", "no errors, successfully ended");
            }
        });
    }

    public static void main(String[] args) {

        // the Crossbar.io router URL and realm we connect to
        String url = "ws://crossbar:8080/ws";
        String realm = "realm1";

        // the executor everything should run on
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // create a session object (that may or may not be reused)
        Session session = new Session(executor);

        // here we define code that is run when the session joins
        session.addOnJoinListener(
            details -> System.out.println("session " + details.sessionID + " joined realm '" + details.realm + "'")
        );
        session.addOnJoinListener(
            details -> run(session, details)
        );

        // create a list of transports we will connect over
        List<ITransport> transports = new ArrayList<>();

        // add a Netty based WAMP-over-WebSocket transport
        transports.add(new NettyWebSocketTransport(executor, url));

        // create a list of authenticators to authenticate our session
        List<IAuthenticator> authenticators = new ArrayList<>();

        // here, we only add "anonymous" as an authenticator
        authenticators.add(new AnonymousAuth(executor));

        // finally, provide everything to a Client instance
        Client client = new Client(executor, transports);

        // and add our session and credentials (in a future version, we
        // might allow adding more than one session here)
        client.add(session, realm, authenticators);

        // now run the client until it is finally done and no longer reconnects
        int exitCode;
        System.out.println("Client.connect() ...");
        CompletableFuture<ExitInfo> exitInfo = client.connect();
        try {
            exitCode = exitInfo.get().code;
        } catch (Exception e) {
            System.out.println(e);
            exitCode = 1;
        }
        System.out.println(".. ended with exit code " + exitCode);

        // exit the program signaling success/failure
        System.exit(exitCode);
    }
}
