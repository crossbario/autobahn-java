package io.crossbar.autobahn.websocket.messages;

import java.util.Map;

/// Initial WebSockets handshake (server response).
public class ServerHandshake extends Message {
    public boolean mSuccess;
    public Map<String, String> headers;

    public ServerHandshake(Map<String, String> headers, boolean success) {
        mSuccess = success;
        this.headers = headers;
    }
}
