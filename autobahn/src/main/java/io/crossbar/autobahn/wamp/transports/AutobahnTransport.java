package io.crossbar.autobahn.wamp.transports;

import java.io.UnsupportedEncodingException;
import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.websocket.WebSocket;
import io.crossbar.autobahn.websocket.WebSocketConnection;

public class AutobahnTransport implements ITransport {

    private final String mUri;
    private final WebSocket mConnection = new WebSocketConnection();
    private static final String[] SERIALIZERS_DEFAULT = new String[] {
            CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME};

    private List<String> mSerializers;

    public AutobahnTransport(String uri) {
        mUri = uri;
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
                    transportHandler.onConnect(AutobahnTransport.this, new CBORSerializer());
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
}
