package io.crossbar.autobahn.websocket.messages;

/// WebSockets binary message to send or received.
public class BinaryMessage extends Message {

    public byte[] mPayload;

    public BinaryMessage(byte[] payload) {
        mPayload = payload;
    }
}
