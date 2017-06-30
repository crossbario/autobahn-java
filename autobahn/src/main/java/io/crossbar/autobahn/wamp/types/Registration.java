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

import io.crossbar.autobahn.wamp.interfaces.IInvocationHandler;


public class Registration {
    public final long registration;
    public final String procedure;
    public final IInvocationHandler endpoint;

    public Registration(long registration, String procedure, IInvocationHandler endpoint) {
        this.registration = registration;
        this.procedure = procedure;
        this.endpoint = endpoint;
    }
}
