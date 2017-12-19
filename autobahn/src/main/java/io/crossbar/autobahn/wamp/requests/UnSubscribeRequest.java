package io.crossbar.autobahn.wamp.requests;

import java.util.concurrent.CompletableFuture;

public class UnSubscribeRequest extends Request {

    public final CompletableFuture<Integer> onReply;
    public final long subscriptionID;

    public UnSubscribeRequest(long request, CompletableFuture<Integer> onReply,
                              long subscriptionID) {
        super(request);
        this.onReply = onReply;
        this.subscriptionID = subscriptionID;
    }
}
