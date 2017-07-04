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

import io.crossbar.autobahn.wamp.interfaces.IEventHandler;
import io.crossbar.autobahn.wamp.types.Subscription;


public class SubscribeRequest extends Request {
    public final String topic;
    public final CompletableFuture<Subscription> onReply;
    public final IEventHandler handler;

    public SubscribeRequest(long request, String topic, CompletableFuture<Subscription> onReply,
                            IEventHandler handler) {
        super(request);
        this.topic = topic;
        this.onReply = onReply;
        this.handler = handler;
    }
}
