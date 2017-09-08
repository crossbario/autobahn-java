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


public interface WebSocket {

    /**
     * Session handler for WebSocket sessions.
     */
    interface ConnectionHandler {

        /**
         * Connection was closed normally.
         */
        int CLOSE_NORMAL = 1;

        /**
         * Connection could not be established in the first place.
         */
        int CLOSE_CANNOT_CONNECT = 2;

        /**
         * A previously established connection was lost unexpected.
         */
        int CLOSE_CONNECTION_LOST = 3;

        /**
         * The connection was closed because a protocol violation
         * occurred.
         */
        int CLOSE_PROTOCOL_ERROR = 4;

        /**
         * Internal error.
         */
        int CLOSE_INTERNAL_ERROR = 5;

        /**
         * Server returned error while connecting
         */
        int CLOSE_SERVER_ERROR = 6;

        /**
         * Server connection lost, scheduled reconnect
         */
        int CLOSE_RECONNECT = 7;

        /**
         * Fired when the WebSockets connection has been established.
         * After this happened, messages may be sent.
         */
        void onOpen();

        /**
         * Fired when the WebSockets connection has deceased (or could
         * not established in the first place).
         *
         * @param code   Close code.
         * @param reason Close reason (human-readable).
         */
        void onClose(int code, String reason);

        /**
         * Fired when a text message has been received (and text
         * messages are not set to be received raw).
         *
         * @param payload Text message payload or null (empty payload).
         */
        void onTextMessage(String payload);

        /**
         * Fired when a text message has been received (and text
         * messages are set to be received raw).
         *
         * @param payload Text message payload as raw UTF-8 or null (empty payload).
         */
        void onRawTextMessage(byte[] payload);

        /**
         * Fired when a binary message has been received.
         *
         * @param payload Binary message payload or null (empty payload).
         */
        void onBinaryMessage(byte[] payload);
    }

    void connect(String wsUri, ConnectionHandler wsHandler) throws WebSocketException;

    void connect(String wsUri, ConnectionHandler wsHandler, WebSocketOptions options) throws WebSocketException;

    void connect(String wsUri, String[] wsSubprotocols, ConnectionHandler wsHandler) throws WebSocketException;

    void disconnect();

    boolean isConnected();

    void sendBinaryMessage(byte[] payload);

    void sendRawTextMessage(byte[] payload);

    void sendTextMessage(String payload);
}
