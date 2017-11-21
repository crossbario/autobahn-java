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

import io.crossbar.autobahn.wamp.types.Publication;


public class PublishRequest extends Request {

    public final CompletableFuture<Publication> onReply;

    public PublishRequest(long request, CompletableFuture<Publication> onReply) {
        super(request);
        this.onReply = onReply;
    }
}
