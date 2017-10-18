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

import java.util.concurrent.atomic.AtomicLong;

public class IDGenerator {
    private AtomicLong mNext = new AtomicLong();

    public long next() {
        // return numbers in the range [0, 2**53-1]
        return mNext.getAndIncrement() & 0x1fffffffffffffL;
    }
}
