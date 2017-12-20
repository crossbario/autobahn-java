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

import java.util.concurrent.CompletableFuture;

public class UnsubscribeRequest extends Request {

    public final CompletableFuture<Integer> onReply;
    public final long subscriptionID;

    public UnsubscribeRequest(long request, CompletableFuture<Integer> onReply,
                              long subscriptionID) {
        super(request);
        this.onReply = onReply;
        this.subscriptionID = subscriptionID;
    }
}
