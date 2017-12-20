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

package io.crossbar.autobahn.wamp.types;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;

public class Subscription {
    public final long subscription;
    public final String topic;
    public final TypeReference resultTypeRef;
    public final Class resultTypeClass;
    public final Object handler;
    public final Session session;

    private boolean active = true;

    public Subscription(long subscription, String topic, TypeReference resultTypeRef,
                        Class resultTypeClass, Object handler, Session session) {
        this.subscription = subscription;
        this.topic = topic;
        this.resultTypeRef = resultTypeRef;
        this.resultTypeClass = resultTypeClass;
        this.handler = handler;
        this.session = session;
    }

    public CompletableFuture<Integer> unsubscribe() {
        return session.unsubscribe(this);
    }

    public void setInactive() {
        if (active) {
            active = false;
        } else {
            throw new IllegalStateException("Subscription already invactive");
        }
    }

    public boolean isActive() {
        return active;
    }
}
