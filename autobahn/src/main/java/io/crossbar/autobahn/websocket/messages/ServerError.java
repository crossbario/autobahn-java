package io.crossbar.autobahn.websocket.messages;

public class ServerError extends Message {
    public int mStatusCode;
    public String mStatusMessage;

    public ServerError(int statusCode, String statusMessage) {
        mStatusCode = statusCode;
        mStatusMessage = statusMessage;
    }

}
