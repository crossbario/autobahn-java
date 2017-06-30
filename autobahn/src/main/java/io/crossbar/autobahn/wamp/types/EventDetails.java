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

import io.crossbar.autobahn.wamp.Session;


public class EventDetails {

    // The subscription on which this event is delivered to.
    public final Subscription subscription;

    // The URI of the topic under the subscription.
    public final String topic;

    // The WAMP sessionid of the publisher.
    public final long publisherSessionID;

    // The WAMP authid of the publisher.
    public final String publisherAuthID;

    // The WAMP authrole of the publisher.
    public final String publisherAuthRole;

    // The WAMP session on which this event is delivered.
    public final Session session;

    public EventDetails(Subscription subscription, String topic, long publisherSessionID,
                             String publisherAuthID, String publisherAuthRole, Session session) {
        this.subscription = subscription;
        this.topic = topic;
        this.publisherSessionID = publisherSessionID;
        this.publisherAuthID = publisherAuthID;
        this.publisherAuthRole = publisherAuthRole;
        this.session = session;
    }
}
