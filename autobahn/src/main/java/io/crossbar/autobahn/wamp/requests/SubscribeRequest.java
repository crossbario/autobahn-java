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

import io.crossbar.autobahn.wamp.types.Subscription;


public class SubscribeRequest extends Request {
    public final String topic;
    public final CompletableFuture<Subscription> onReply;
    public final TypeReference resultType;
    public final Object handler;

    public SubscribeRequest(long request, String topic, CompletableFuture<Subscription> onReply,
                            TypeReference resultType, Object handler) {
        super(request);
        this.topic = topic;
        this.onReply = onReply;
        this.resultType = resultType;
        this.handler = handler;
    }
}
