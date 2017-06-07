package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.Subscription;

public class Playground {

    private Session mSession;
    private static final String PROCEDURE = "com.myapp.hello";

    public Playground() {
        mSession = new Session();
    }

    private void showMePubSubPattern() {
        // Subscribe to a topic
        CompletableFuture<Subscription> subscription = mSession.subscribe(PROCEDURE, this::onHello, null);

        // Publish to a topic
        List<Object> args = new ArrayList<>();
        args.add("crossbar");
        args.add("something");
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("Name", "Crossbar.io");
        kwargs.put("Protocol", "WAMP");
        kwargs.put("ProtocolVersion", 1);
        CompletableFuture<Publication> publicationResult = mSession.publish(PROCEDURE, args, kwargs, null);

        // Register a procedure.
        CompletableFuture<Registration> registration = mSession.register(PROCEDURE, this::add2, null);

        // Call a procedure
        List<Object> args1 = new ArrayList<>();
        args1.add(1);
        args1.add(2);
        CompletableFuture<CallResult> callResult = mSession.call(PROCEDURE, args1, null, null);
    }

    private void showMeObserverPattern() {
        Session.OnJoinListener onJoinListener = mSession.registerOnJoinListener(
                details -> System.out.println("play with join details here"));
        mSession.unregisterOnJoinListener(onJoinListener);

        Session.OnLeaveListener onLeaveListener = mSession.registerOnLeaveListener(
                details -> System.out.println("play with close details here"));
        mSession.unregisterOnLeaveListener(onLeaveListener);

        Session.OnConnectListener onConnectListener = mSession.registerOnConnectListener(
                () -> System.out.println("Do stuff after connect."));
        mSession.unregisterOnConnectListener(onConnectListener);

        Session.OnDisconnectListener onDisconnectListener = mSession.registerOnDisconnectListener(
                () -> System.out.println("Do stuff after disconnect."));
        mSession.unregisterOnDisconnectListener(onDisconnectListener);
    }

    private Void onHello(List<Object> args, Map<String, Object> kwargs){
        System.out.println(String.format("Got event: %s", args.toString()));
        return null;
    }

    private int add2(List<Object> args, Map<String, Object> kwargs) {
        return (int) args.get(0) + (int) args.get(1);
    }
}
