package io.crossbar.autobahn.demogallery.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.NettyTransport;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;

public class EchoClient {
    private Client mClient;
    private Session mSession;

    public EchoClient(String uri, String realm) {
        mSession = new Session();
        List<ITransport> transportList = new ArrayList<>();
        transportList.add(new NettyTransport(uri));
        mSession.addOnJoinListener(details -> funStuff());
        mClient = new Client(mSession, transportList, realm, null);
    }

    public void funStuff() {
        System.out.println("JOINED");
        CallOptions options = new CallOptions(5);
        CompletableFuture<CallResult> resultCompletableFuture = mSession.call(
                "com.byteshaft.grab_screenshot", null, null, options);
        resultCompletableFuture.thenAccept(callResult -> {
            System.out.println(callResult.results.get(0));
            System.out.println(callResult.kwresults);
        });
//        mSession.subscribe("com.byteshaft.topic1", this::message, null);
//        mSession.register("com.byteshaft.exp", this::exp, null);
    }

    private CompletableFuture<InvocationResult> exp(List<Object> args, Map<String, Object> kwargs,
                                                    InvocationDetails details) {
        CompletableFuture<InvocationResult> future = new CompletableFuture<>();
        System.out.println("Called");
        return future;

    }

    private Void message(List<Object> args, Map<String, Object> kwargs) {
        System.out.println(args);
        System.out.println(kwargs);
        return null;
    }

    public void start() {
        CompletableFuture<ExitInfo> exitInfoCompletableFuture = mClient.connect();
        exitInfoCompletableFuture.thenApply(exitInfo -> mClient.connect());
    }
}
