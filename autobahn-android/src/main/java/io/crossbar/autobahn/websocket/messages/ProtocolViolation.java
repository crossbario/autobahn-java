package io.crossbar.autobahn.websocket.messages;

import io.crossbar.autobahn.websocket.exceptions.WebSocketException;

/// WebSockets reader detected WS protocol violation.
public class ProtocolViolation extends Message {

    public WebSocketException mException;

    public ProtocolViolation(WebSocketException e) {
        mException = e;
    }
}
