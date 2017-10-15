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

public class Subscription {
    public final long subscription;
    public final String topic;
    public final TypeReference resultTypeRef;
    public final Class resultTypeClass;
    public final Object handler;

    public Subscription(long subscription, String topic, TypeReference resultTypeRef,
                        Class resultTypeClass, Object handler) {
        this.subscription = subscription;
        this.topic = topic;
        this.resultTypeRef = resultTypeRef;
        this.resultTypeClass = resultTypeClass;
        this.handler = handler;
    }
}
