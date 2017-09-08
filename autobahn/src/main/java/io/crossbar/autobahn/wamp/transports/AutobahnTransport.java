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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.wamp.types.WebSocketOptions;
import io.crossbar.autobahn.websocket.WebSocket;
import io.crossbar.autobahn.websocket.WebSocketConnection;

public class AutobahnTransport implements ITransport {

    private static final Logger LOGGER = Logger.getLogger(AutobahnTransport.class.getName());
    private static final String[] SERIALIZERS_DEFAULT = new String[] {
            CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME};

    private final WebSocketConnection mConnection;
    private final String mUri;

    private ExecutorService mExecutor;
    private WebSocketOptions mOptions;
    private List<String> mSerializers;

    public AutobahnTransport(String uri) {
        mUri = uri;
        mConnection = new WebSocketConnection();
    }

    public AutobahnTransport(String uri, List<String> serializers) {
        this(uri);
        mSerializers = serializers;
    }

    public AutobahnTransport(String uri, WebSocketOptions options) {
        this(uri);
        mOptions = options;
    }

    public AutobahnTransport(String uri, ExecutorService executor) {
        this(uri);
        mExecutor = executor;
    }

    public AutobahnTransport(String uri, List<String> serializers, ExecutorService executor) {
        this(uri);
        mExecutor = executor;
        mSerializers = serializers;
    }

    public AutobahnTransport(String uri, ExecutorService executor, WebSocketOptions options) {
        this(uri);
        mExecutor = executor;
        mOptions = options;
    }

    private ExecutorService getExecutor() {
        return mExecutor == null ? ForkJoinPool.commonPool() : mExecutor;
    }

    private WebSocketOptions getOptions() {
        return mOptions == null ? new WebSocketOptions() : mOptions;
    }

    private String[] getSerializers() {
        if (mSerializers != null) {
            return (String[]) mSerializers.toArray();
        }
        return SERIALIZERS_DEFAULT;
    }

    @Override
    public void send(byte[] payload, boolean isBinary) {
        if (isBinary) {
            mConnection.sendBinaryMessage(payload);
        } else {
            try {
                mConnection.sendTextMessage(new String(payload, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connect(ITransportHandler transportHandler) throws Exception {
        mConnection.connect(mUri, getSerializers(), new WebSocket.ConnectionHandler() {
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
            public void onTextMessage(String payload) {
                try {
                    transportHandler.onMessage(payload.getBytes(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRawTextMessage(byte[] payload) {

            }

            @Override
            public void onBinaryMessage(byte[] payload) {
                try {
                    transportHandler.onMessage(payload, true);
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
        mConnection.disconnect();
    }

    @Override
    public void abort() throws Exception {
        mConnection.disconnect();
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
