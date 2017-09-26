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

package io.crossbar.autobahn.websocket.interfaces;


import java.util.Map;

import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;

public interface IWebSocket {

    void connect(String wsUri, IWebSocketConnectionHandler wsHandler) throws WebSocketException;

    void connect(String wsUri, IWebSocketConnectionHandler wsHandler,
                 WebSocketOptions options) throws WebSocketException;

    void connect(String wsUri, String[] wsSubprotocols, IWebSocketConnectionHandler wsHandler)
            throws WebSocketException;

    void connect(String wsUri, String[] wsSubprotocols, IWebSocketConnectionHandler wsHandler,
                 WebSocketOptions options, Map<String, String> headers) throws WebSocketException;

    void sendClose();

    void sendClose(int code);

    void sendClose(int code, String reason);

    boolean isConnected();

    void sendMessage(String payload);

    void sendMessage(byte[] payload, boolean isBinary);

    void sendPing();

    void sendPing(byte[] payload);

    void sendPong();

    void sendPong(byte[] payload);
}
