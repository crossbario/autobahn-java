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

public class PublishOptions {
    public final boolean acknowledge;
    public final boolean excludeMe;

    public PublishOptions(boolean acknowledge, boolean excludeMe) {
        this.acknowledge = acknowledge;
        this.excludeMe = excludeMe;
    }
}
