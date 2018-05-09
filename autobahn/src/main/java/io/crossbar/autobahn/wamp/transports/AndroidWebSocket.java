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

package io.crossbar.autobahn.wamp.transports;

import java.util.List;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.TransportOptions;
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;

public class AndroidWebSocket implements ITransport {

    public static final IABLogger LOGGER = ABLogger.getLogger(AndroidWebSocket.class.getName());
    private static final String[] SERIALIZERS_DEFAULT = new String[] {
            CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME};

    private final WebSocketConnection mConnection;
    private final String mUri;

    private List<String> mSerializers;
    private ISerializer mSerializer;

    public AndroidWebSocket(String uri) {
        mUri = uri;
        mConnection = new WebSocketConnection();
    }

    public AndroidWebSocket(String uri, List<String> serializers) {
        this(uri);
        mSerializers = serializers;
    }

    private String[] getSerializers() {
        if (mSerializers != null) {
            return (String[]) mSerializers.toArray();
        }
        return SERIALIZERS_DEFAULT;
    }

    @Override
    public void send(byte[] payload, boolean isBinary) {
        mConnection.sendMessage(payload, isBinary);
    }

    @Override
    public void connect(ITransportHandler transportHandler) throws Exception {
        connect(transportHandler, new TransportOptions());
    }

    @Override
    public void connect(ITransportHandler transportHandler, TransportOptions options)
            throws Exception {

        WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setAutoPingInterval(options.getAutoPingInterval());
        webSocketOptions.setAutoPingTimeout(options.getAutoPingTimeout());
        webSocketOptions.setMaxFramePayloadSize(options.getMaxFramePayloadSize());

        mConnection.connect(mUri, getSerializers(), new WebSocketConnectionHandler() {

            @Override
            public void onConnect(ConnectionResponse response) {
                LOGGER.d(String.format("Negotiated serializer=%s", response.protocol));
                try {
                    mSerializer = initializeSerializer(response.protocol);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onOpen() {
                try {
                    transportHandler.onConnect(AndroidWebSocket.this, mSerializer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason) {
                String closeReason;
                if (code == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST) {
                    closeReason = CloseDetails.REASON_TRANSPORT_LOST;
                } else {
                    closeReason = CloseDetails.REASON_DEFAULT;
                }
                transportHandler.onLeave(new CloseDetails(closeReason, null));
                LOGGER.d(String.format("Disconnected, code=%s, reasons=%s", code, reason));
                transportHandler.onDisconnect(code == IWebSocketConnectionHandler.CLOSE_NORMAL
                        || code == 1000);
            }

            @Override
            public void onMessage(String payload) {
                try {
                    transportHandler.onMessage(payload.getBytes(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(byte[] payload, boolean isBinary) {
                try {
                    transportHandler.onMessage(payload, isBinary);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, webSocketOptions, null);
    }

    @Override
    public boolean isOpen() {
        return mConnection.isConnected();
    }

    @Override
    public void close() throws Exception {
        mConnection.sendClose();
    }

    @Override
    public void abort() throws Exception {
        mConnection.sendClose();
    }

    @Override
    public void setOptions(TransportOptions options) {
        WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setAutoPingTimeout(options.getAutoPingTimeout());
        webSocketOptions.setAutoPingInterval(options.getAutoPingInterval());
        mConnection.setOptions(webSocketOptions);
    }

    private ISerializer initializeSerializer(String negotiatedSerializer) throws Exception {
        switch (negotiatedSerializer) {
            case CBORSerializer.NAME:
                return new CBORSerializer();
            case JSONSerializer.NAME:
                return new JSONSerializer();
            case MessagePackSerializer.NAME:
                return new MessagePackSerializer();
            default:
                throw new IllegalArgumentException("Unsupported serializer.");
        }
    }
}
