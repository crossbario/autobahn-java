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

import java.util.concurrent.CompletableFuture;

import io.crossbar.autobahn.wamp.Session;

public class Registration {
    public final long registration;
    public final String procedure;
    public final Object endpoint;
    public final Session session;

    private boolean active = true;

    public Registration(long registration, String procedure, Object endpoint, Session session) {
        this.registration = registration;
        this.procedure = procedure;
        this.endpoint = endpoint;
        this.session = session;
    }

    public CompletableFuture<Integer> unregister() {
        return session.unregister(this);
    }

    public void setInactive() {
        if (active) {
            active = false;
        } else {
            throw new IllegalStateException("Registration already invactive");
        }
    }

    public boolean isActive() {
        return active;
    }
}
