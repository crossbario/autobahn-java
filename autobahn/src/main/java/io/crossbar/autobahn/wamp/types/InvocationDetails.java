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
import io.crossbar.autobahn.wamp.interfaces.Progress;

public class InvocationDetails {

    // The registration on which this invocation is for.
    public final Registration registration;

    // The URI of the procedure invoked under the registration.
    public final String procedure;

    // The WAMP sessionid of the caller.
    public final long callerSessionID;

    // The WAMP authid of the caller.
    public final String callerAuthID;

    // The WAMP authRole of the caller.
    public final String callerAuthRole;

    // The WAMP session on which this event is delivered.
    public final Session session;

    // callback produce progressive results.
    public final Progress progress;

    // XXXX - Tentative, the constructor parameter order may change.
    public InvocationDetails(Registration registration, String procedure, long callerSessionID,
                             String callerAuthID, String callerAuthRole, Session session, Progress progress) {
        this.registration = registration;
        this.procedure = procedure;
        this.callerSessionID = callerSessionID;
        this.callerAuthID = callerAuthID;
        this.callerAuthRole = callerAuthRole;
        this.session = session;
        this.progress = progress;
    }
}
