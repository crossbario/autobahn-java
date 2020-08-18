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

public class SessionDetails {
    public final long sessionID;
    public final String realm;
    public final String authid;
    public final String authrole;
    public final String authmethod;

    public SessionDetails(String realm, long sessionID, String authid, String authrole,
                          String authmethod) {
        this.realm = realm;
        this.sessionID = sessionID;
        this.authid = authid;
        this.authrole = authrole;
        this.authmethod = authmethod;
    }
}
