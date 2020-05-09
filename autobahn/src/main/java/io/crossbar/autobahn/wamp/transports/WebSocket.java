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

package io.crossbar.autobahn.wamp.transports;

import java.util.List;

public class WebSocket extends AndroidWebSocket {
    public WebSocket(String uri) {
        super(uri);
    }

    public WebSocket(String uri, List<String> serializers) {
        super(uri, serializers);
    }
}
