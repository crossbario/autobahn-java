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

package io.crossbar.autobahn;

/**
 * WebSockets event handler. Users will usually provide an instance of a class
 * derived from this to handle WebSockets received messages and open/close events
 */
public class WebSocketConnectionHandler implements WebSocket.ConnectionHandler {

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

    /**
     * Fired when a text message has been received (and text
     * messages are not set to be received raw).
     *
     * @param payload Text message payload or null (empty payload).
     */
    public void onTextMessage(String payload) {
    }

    /**
     * Fired when a text message has been received (and text
     * messages are set to be received raw).
     *
     * @param payload Text message payload as raw UTF-8 or null (empty payload).
     */
    public void onRawTextMessage(byte[] payload) {
    }

    /**
     * Fired when a binary message has been received.
     *
     * @param payload Binar message payload or null (empty payload).
     */
    public void onBinaryMessage(byte[] payload) {
    }

}
