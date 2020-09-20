package io.crossbar.autobahn.websocket.messages;

public class ServerError extends Message {
    public String mStatusMessage;

    public ServerError(String statusMessage) {
        mStatusMessage = statusMessage;
    }
}
