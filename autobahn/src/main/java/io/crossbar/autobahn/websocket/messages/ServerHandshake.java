package io.crossbar.autobahn.websocket.messages;

import java.util.Map;

/// Initial WebSockets handshake (server response).
public class ServerHandshake extends Message {
    public Map<String, String> headers;

    public ServerHandshake(Map<String, String> headers) {
        this.headers = headers;
    }
}
