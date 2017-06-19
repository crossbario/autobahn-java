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

public class ExitInfo {
    public final int code;

    public ExitInfo() {
        code = 0;
    }

    public ExitInfo(boolean wasClean) {
        if (wasClean) {
            code = 0;
        } else {
            code = 1;
        }
    }

    public ExitInfo(int _code) {
        code = _code;
    }
}
