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

package io.crossbar.autobahn.websocket;


public class WebSocketException extends Exception {

    private static final long serialVersionUID = 1L;

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable t) {
        super(message, t);
    }
}
