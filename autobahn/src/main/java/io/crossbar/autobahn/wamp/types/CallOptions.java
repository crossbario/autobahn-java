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

import io.crossbar.autobahn.wamp.interfaces.ProgressHandler;

public class CallOptions {
    public int timeout;
    public ProgressHandler progressHandler;

    public CallOptions(int timeout) {
        this.timeout = timeout;
    }

    public CallOptions(ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    public CallOptions(int timeout, ProgressHandler progressHandler) {
        this.timeout = timeout;
        this.progressHandler = progressHandler;
    }
}
