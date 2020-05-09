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
