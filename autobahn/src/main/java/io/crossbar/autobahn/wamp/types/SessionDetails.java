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
    public long sessionID;
    public String realm;
    public String authid;
    public String authrole;

    public SessionDetails(String realm, long sessionID) {
        this.realm = realm;
        this.sessionID = sessionID;
    }
}
