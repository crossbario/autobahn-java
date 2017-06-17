package io.crossbar.autobahn.wamp.requests;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;
import io.crossbar.autobahn.wamp.types.CallResult;

public class CallRequest extends Request {
    public final String procedure;
    public final CallOptions options;
    public final CompletableFuture<CallResult> onReply;

    public CallRequest(long request, String procedure, CompletableFuture<CallResult> onReply,
                       CallOptions options) {
        super(request);
        this.procedure = procedure;
        this.options = options;
        this.onReply = onReply;
    }
}
