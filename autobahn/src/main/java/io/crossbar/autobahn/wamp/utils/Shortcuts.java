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

package io.crossbar.autobahn.wamp.utils;

import java.util.Map;

public class Shortcuts {
    public static <T> T getOrDefault(Map<?, ?> obj, Object key, T default_value) {
        if (obj.containsKey(key)) {
            return (T) obj.get(key);
        }
        return default_value;
    }
}
