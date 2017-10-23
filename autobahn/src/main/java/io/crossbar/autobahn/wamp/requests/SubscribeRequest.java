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

import io.crossbar.autobahn.wamp.types.Subscription;


public class SubscribeRequest extends Request {
    public final String topic;
    public final CompletableFuture<Subscription> onReply;
    public final TypeReference resultTypeRef;
    public final Class resultTypeClass;
    public final Object handler;

    public SubscribeRequest(long request, String topic, CompletableFuture<Subscription> onReply,
                            TypeReference resultTypeRef, Class resultTypeClass, Object handler) {
        super(request);
        this.topic = topic;
        this.onReply = onReply;
        if (resultTypeRef != null && resultTypeClass != null) {
            throw new IllegalArgumentException(
                    "Can only provide one of resultTypeRef or resultTypeClass");
        }
        this.resultTypeRef = resultTypeRef;
        this.resultTypeClass = resultTypeClass;
        this.handler = handler;
    }
}
