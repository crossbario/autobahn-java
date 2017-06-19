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

import java.util.Map;

public class Challenge {
    public final String method;
    public final Map<String, Object> extra;

    public Challenge(String method, Map<String, Object> extra) {
        this.method = method;
        this.extra = extra;
    }
}
