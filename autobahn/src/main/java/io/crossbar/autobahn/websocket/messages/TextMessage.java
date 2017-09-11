package io.crossbar.autobahn.websocket.messages;

/// WebSockets text message to send or received.
public class TextMessage extends Message {

    public String mPayload;

    public TextMessage(String payload) {
        mPayload = payload;
    }
}
