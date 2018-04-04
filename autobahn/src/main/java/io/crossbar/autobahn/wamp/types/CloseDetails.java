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

public class CloseDetails {

    public static final String REASON_DEFAULT = "wamp.close.normal";
    public static final String REASON_TRANSPORT_LOST = "wamp.close.transport_lost";

    public final String reason;
    public final String message;

    public CloseDetails(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }
}
