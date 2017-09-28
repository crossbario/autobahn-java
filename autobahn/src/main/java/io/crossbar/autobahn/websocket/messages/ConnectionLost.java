package io.crossbar.autobahn.websocket.messages;

/// WebSockets connection lost
public class ConnectionLost extends Message {
    public final String reason;

    public ConnectionLost(String reason) {
        if (reason == null) {
            this.reason = "WebSockets connection lost";
        } else {
            this.reason = reason;
        }
    }
}
