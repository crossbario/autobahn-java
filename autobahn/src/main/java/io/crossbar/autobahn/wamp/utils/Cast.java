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

public class Cast {
    public static long castRequestID(Object object) {
        try {
            return (int) object;
        } catch (ClassCastException ignore) {
            return (long) object;
        }
    }
}
