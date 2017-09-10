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
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;

public class AutobahnTransport implements ITransport {

    private static final Logger LOGGER = Logger.getLogger(AutobahnTransport.class.getName());
    private static final String[] SERIALIZERS_DEFAULT = new String[] {
            CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME};

    private final WebSocketConnection mConnection;
    private final String mUri;

    private List<String> mSerializers;

    public AutobahnTransport(String uri) {
        mUri = uri;
        mConnection = new WebSocketConnection();
    }

    public AutobahnTransport(String uri, List<String> serializers) {
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
        mConnection.connect(mUri, getSerializers(), new WebSocketConnectionHandler() {
            @Override
            public void onOpen() {
                try {
                    String negotiatedSerializer = mConnection.getHandshakeResponseHeaders().get("Sec-WebSocket-Protocol");
                    LOGGER.info(String.format("Negotiated serializer=%s", negotiatedSerializer));
                    ISerializer serializer = initializeSerializer(negotiatedSerializer);
                    transportHandler.onConnect(AutobahnTransport.this, serializer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason) {
                transportHandler.onDisconnect(code == 1000);
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
        });
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
