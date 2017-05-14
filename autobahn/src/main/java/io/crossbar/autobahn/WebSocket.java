/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package io.crossbar.autobahn;

public interface WebSocket {

    /**
     * Session handler for WebSocket sessions.
     */
    public interface ConnectionHandler {

        /**
         * Connection was closed normally.
         */
        public static final int CLOSE_NORMAL = 1;

        /**
         * Connection could not be established in the first place.
         */
        public static final int CLOSE_CANNOT_CONNECT = 2;

        /**
         * A previously established connection was lost unexpected.
         */
        public static final int CLOSE_CONNECTION_LOST = 3;

        /**
         * The connection was closed because a protocol violation
         * occurred.
         */
        public static final int CLOSE_PROTOCOL_ERROR = 4;

        /**
         * Internal error.
         */
        public static final int CLOSE_INTERNAL_ERROR = 5;

        /**
         * Server returned error while connecting
         */
        public static final int CLOSE_SERVER_ERROR = 6;

        /**
         * Server connection lost, scheduled reconnect
         */
        public static final int CLOSE_RECONNECT = 7;

        /**
         * Fired when the WebSockets connection has been established.
         * After this happened, messages may be sent.
         */
        public void onOpen();

        /**
         * Fired when the WebSockets connection has deceased (or could
         * not established in the first place).
         *
         * @param code   Close code.
         * @param reason Close reason (human-readable).
         */
        public void onClose(int code, String reason);

        /**
         * Fired when a text message has been received (and text
         * messages are not set to be received raw).
         *
         * @param payload Text message payload or null (empty payload).
         */
        public void onTextMessage(String payload);

        /**
         * Fired when a text message has been received (and text
         * messages are set to be received raw).
         *
         * @param payload Text message payload as raw UTF-8 or null (empty payload).
         */
        public void onRawTextMessage(byte[] payload);

        /**
         * Fired when a binary message has been received.
         *
         * @param payload Binar message payload or null (empty payload).
         */
        public void onBinaryMessage(byte[] payload);
    }

    public void connect(String wsUri, ConnectionHandler wsHandler) throws WebSocketException;

    public void connect(String wsUri, ConnectionHandler wsHandler, WebSocketOptions options) throws WebSocketException;

    public void disconnect();

    public boolean isConnected();

    public void sendBinaryMessage(byte[] payload);

    public void sendRawTextMessage(byte[] payload);

    public void sendTextMessage(String payload);
}
