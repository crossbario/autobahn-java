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
    public final boolean retain;

    public PublishOptions(boolean acknowledge, boolean excludeMe) {
    	this(acknowledge, excludeMe, false);
    }
    
    public PublishOptions(boolean acknowledge, boolean excludeMe, boolean retain) {
        this.acknowledge = acknowledge;
        this.excludeMe = excludeMe;
        this.retain = retain;
    }
}
