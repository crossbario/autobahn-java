///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.websocket;


import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;

/**
 * WebSockets event handler. Users will usually provide an instance of a class
 * derived from this to handle WebSockets received messages and open/close events
 */
public class WebSocketConnectionHandler implements IWebSocketConnectionHandler {

    private WebSocketConnection mConnection;

    @Override
    public void onConnect(ConnectionResponse response) {

    }

    /**
     * Fired when the WebSockets connection has been established.
     * After this happened, messages may be sent.
     */
    public void onOpen() {

    }

    /**
     * Fired when the WebSockets connection has deceased (or could
     * not established in the first place).
     *
     * @param code   Close code.
     * @param reason Close reason (human-readable).
     */
    public void onClose(int code, String reason) {

    }

    @Override
    public void onMessage(String payload) {

    }

    @Override
    public void onMessage(byte[] payload, boolean isBinary) {

    }

    @Override
    public void onPing() {
        mConnection.sendPong();
    }

    @Override
    public void onPing(byte[] payload) {
        mConnection.sendPong(payload);
    }

    @Override
    public void onPong() {

    }

    @Override
    public void onPong(byte[] payload) {

    }

    @Override
    public void setConnection(WebSocketConnection connection) {
        mConnection = connection;
    }
}
