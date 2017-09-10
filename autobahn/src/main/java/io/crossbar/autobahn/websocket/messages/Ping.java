package io.crossbar.autobahn.websocket.messages;

/// WebSockets ping to send or received.
public class Ping extends Message {

    public byte[] mPayload;

    Ping() {
        mPayload = null;
    }

    public Ping(byte[] payload) {
        mPayload = payload;
    }
}
