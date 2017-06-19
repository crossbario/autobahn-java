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

public class ChallengeResponse {
    public final String signature;
    public final Map<String, Object> extra;

    public ChallengeResponse(String signature, Map<String, Object> extra) {
        this.signature = signature;
        this.extra = extra;
    }
}
