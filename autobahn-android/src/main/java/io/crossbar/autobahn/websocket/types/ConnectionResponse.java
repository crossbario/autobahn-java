package io.crossbar.autobahn.websocket.types;

public class ConnectionResponse {

    public final String protocol;

    public ConnectionResponse(String protocol) {
        this.protocol = protocol;
    }
}
