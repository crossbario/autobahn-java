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

public class RegisterOptions {
    public static final String MATCH_EXACT = "exact";
    public static final String MATCH_PREFIX = "prefix";
    public static final String MATCH_WILDCARD = "wildcard";

    public final static String INVOKE_SINGLE = "single";
    public final static String INVOKE_FIRST = "first";
    public final static String INVOKE_LAST = "last";
    public final static String INVOKE_ROUNDROBIN = "roundrobin";
    public final static String INVOKE_RANDOM = "random";
    public final static String INVOKE_ALL = "all";

    public String match;
    public String invoke;

    public RegisterOptions(String match, String invoke) {
        this.match = match;
        this.invoke = invoke;
    }

    public Map<String, Object> message_attr() {
        // TODO: implement.
        return null;
    }
}
