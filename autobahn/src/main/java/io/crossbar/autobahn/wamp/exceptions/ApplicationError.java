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

import java.util.List;
import java.util.Map;

public class ApplicationError extends Error {

    public final List<Object> args;
    public final Map<String, Object> kwargs;

    public ApplicationError(String message, List<Object> args, Map<String, Object> kwargs) {
        super(message);
        this.args = args;
        this.kwargs = kwargs;
    }
}
