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

package io.crossbar.autobahn.wamp.exceptions;

public class ApplicationError extends Error {

    // # FIXME: can be extended from here, currently only notify error URI.
    public ApplicationError(String message) {
        super(message);
    }
}
