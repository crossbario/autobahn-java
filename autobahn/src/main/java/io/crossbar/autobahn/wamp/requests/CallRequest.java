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

import java8.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.CallOptions;


public class CallRequest extends Request {
    public final String procedure;
    public final CallOptions options;
    public final CompletableFuture onReply;
    public final TypeReference resultTypeRef;
    public final Class resultTypeClass;

    public CallRequest(long request, String procedure, CompletableFuture onReply,
                       CallOptions options, TypeReference resultTypeRef, Class resultTypeClass) {
        super(request);
        this.procedure = procedure;
        this.options = options;
        this.onReply = onReply;
        if (resultTypeRef != null && resultTypeClass != null) {
            throw new IllegalArgumentException(
                    "Can only provide one of resultTypeRef or resultTypeClass");
        }
        this.resultTypeRef = resultTypeRef;
        this.resultTypeClass = resultTypeClass;
    }
}
