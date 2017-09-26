package io.crossbar.autobahn.websocket.interfaces;

import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;

/**
 * Session handler for WebSocket sessions.
 */
public interface IWebSocketConnectionHandler {
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

    void onConnect(ConnectionResponse response);

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

    void onMessage(String payload);

    void onMessage(byte[] payload, boolean isBinary);

    void onPing();

    void onPing(byte[] payload);

    void onPong();

    void onPong(byte[] payload);

    void setConnection(WebSocketConnection connection);
}
