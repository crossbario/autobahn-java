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

import java8.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.types.Registration;


public class RegisterRequest extends Request {
    public final CompletableFuture<Registration> onReply;
    public final String procedure;
    public final Object endpoint;

    public RegisterRequest(long request, CompletableFuture<Registration> onReply, String procedure,
                           Object endpoint) {
        super(request);
        this.onReply = onReply;
        this.procedure = procedure;
        this.endpoint = endpoint;
    }
}
