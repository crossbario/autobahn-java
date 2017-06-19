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

public class IDGenerator {
    private long mNext;

    public long next() {
        mNext += 1;
        if (mNext > 9007199254740992L) {
            mNext = 1;
        }
        return mNext;
    }
}
