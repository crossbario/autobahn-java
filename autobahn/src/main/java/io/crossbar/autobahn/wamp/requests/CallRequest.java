///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.wamp.requests;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;

public class CallRequest extends Request {
    public final String procedure;
    public final CallOptions options;
    public final CompletableFuture onReply;
    public final TypeReference resultType;

    public CallRequest(long request, String procedure, CompletableFuture onReply, CallOptions options,
                       TypeReference resultType) {
        super(request);
        this.procedure = procedure;
        this.options = options;
        this.onReply = onReply;
        this.resultType = resultType;
    }
}
