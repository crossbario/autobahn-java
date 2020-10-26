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

import java.util.HashMap;
import java.util.Map;

public class SubscribeOptions extends HashMap<String, Object> {

    public static final String KEY_MATCH = "match";
    public static final String KEY_GET_RETAINED = "get_retained";

    public SubscribeOptions(String match, boolean getRetained) {
        putMatch(match);
        putGetRetained(getRetained);
    }

    public SubscribeOptions(Map<String, Object> origin) {
        super(origin);
    }

    public SubscribeOptions() {
        super();
    }

    public void putMatch(String match) {
        put(KEY_MATCH, match);
    }

    public void putGetRetained(boolean getRetained) {
        put(KEY_GET_RETAINED, getRetained);
    }

    public String getMatch() {
        Object value = get(KEY_MATCH);
        return value instanceof String ? (String) value : null;
    }

    public Boolean getRetained() {
        Object value = get(KEY_GET_RETAINED);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public void removeMatch() {
        remove(KEY_MATCH);
    }

    public void removeGetRetained() {
        remove(KEY_GET_RETAINED);
    }

}
