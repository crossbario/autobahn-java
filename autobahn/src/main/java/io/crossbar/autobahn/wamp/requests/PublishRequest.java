package io.crossbar.autobahn.wamp.requests;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.Publication;

public class PublishRequest extends Request {

    public final CompletableFuture<Publication> onReply;

    public PublishRequest(long request, CompletableFuture<Publication> onReply) {
        super(request);
        this.onReply = onReply;
    }
}
