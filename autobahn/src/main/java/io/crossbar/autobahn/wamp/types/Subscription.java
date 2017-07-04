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

public class Subscription {
    public final long subscription;
    public final String topic;
    public final Object handler;

    public Subscription(long subscription, String topic, Object handler) {
        this.subscription = subscription;
        this.topic = topic;
        this.handler = handler;
    }
}
