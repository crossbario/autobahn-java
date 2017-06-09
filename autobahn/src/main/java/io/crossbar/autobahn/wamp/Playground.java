package io.crossbar.autobahn.wamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.Hello;
import io.crossbar.autobahn.wamp.types.Message;
import io.crossbar.autobahn.wamp.types.Publication;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.Subscription;

public class Playground {

    private Session mSession;
    private static final String PROCEDURE = "com.myapp.hello";

    public Playground() {
        mSession = new Session();
    }

    private void showTransportAttachment() {
        class AFakeTransport implements ITransport {

            @Override
            public void send(Message message) {

            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void close() {

            }

            @Override
            public void abort() {

            }

            @Override
            public int getChannelID() {
                return 0;
            }
        }

        AFakeTransport transport = new AFakeTransport();

        mSession.attachTransport(transport);
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
        Session.OnJoinListener onJoinListener = mSession.addOnJoinListener(
                details -> System.out.println("play with join details here"));
        mSession.removeOnJoinListener(onJoinListener);

        Session.OnLeaveListener onLeaveListener = mSession.addOnLeaveListener(
                details -> System.out.println("play with close details here"));
        mSession.removeOnLeaveListener(onLeaveListener);

        Session.OnConnectListener onConnectListener = mSession.addOnConnectListener(
                () -> System.out.println("Do stuff after connect."));
        mSession.removeOnConnectListener(onConnectListener);

        Session.OnDisconnectListener onDisconnectListener = mSession.addOnDisconnectListener(
                () -> System.out.println("Do stuff after disconnect."));
        mSession.removeOnDisconnectListener(onDisconnectListener);

        Session.OnChallengeListener onChallengeListener = mSession.addOnChallengeListener(
                challenge -> System.out.println("play with challenge here."));
        mSession.removeOnChallengeListener(onChallengeListener);

        Session.OnUserErrorListener onUserErrorListener = mSession.addOnUserErrorListener(
                message -> System.out.println("play with user error here."));
        mSession.removeOnUserErrorListener(onUserErrorListener);
    }

    private Void onHello(List<Object> args, Map<String, Object> kwargs){
        System.out.println(String.format("Got event: %s", args.toString()));
        return null;
    }

    private int add2(List<Object> args, Map<String, Object> kwargs) {
        return (int) args.get(0) + (int) args.get(1);
    }
}
