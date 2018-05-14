package io.crossbar.autobahn.wamp.transports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import javax.net.SocketFactory;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.types.TransportOptions;


// FIXME: NOT READY, IS WORK-IN-PROGRESS
public class RawSocket implements ITransport {

    private static final int ZERO = 0x00;

    private Socket mSocket;
    private String mUri;
    private OutputStream mOStream;
    private InputStream mIStream;
    private ISerializer mSerializer;

    public RawSocket(String uri) {
        mUri = uri;
        throw new IllegalStateException("NOT IMPLEMENTED YET");
    }

    @Override
    public void send(byte[] payload, boolean isBinary) {
        try {
            mOStream.write(new byte[] {ZERO, ZERO, ZERO, (byte) payload.length});
            mOStream.write(payload);
            mOStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect(ITransportHandler transportHandler) throws Exception {
        connect(transportHandler, null);
    }

    @Override
    public void connect(ITransportHandler transportHandler, TransportOptions options) throws Exception {
        mSocket = SocketFactory.getDefault().createSocket();
        URI uri = URI.create(mUri);
        mSocket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
        mOStream = mSocket.getOutputStream();
        mIStream = mSocket.getInputStream();

        int serializerID = JSONSerializer.RAWSOCKET_SERIALIZER_ID;
        int request_max_len_exp = 24;
        long maxSize = (long) Math.pow(2, 9 + request_max_len_exp);

        mOStream.write(0x7f);
        mOStream.write((request_max_len_exp - 9) << 4 | JSONSerializer.RAWSOCKET_SERIALIZER_ID);
        mOStream.write(ZERO);
        mOStream.write(ZERO);
        mOStream.flush();

        byte[] bytes = new byte[4];
        while (true) {
            mIStream.read(bytes);
            byte b = bytes[1];

            int serializer = b & 0x0F;
            int max_length = b >> 4 & 0x0F;

            if (max_length + 9 == request_max_len_exp && serializer == serializerID) {
                System.out.println("MAX " +  maxSize);
                System.out.println("Handshake complete, lets roll");
                mSerializer = new JSONSerializer();
                transportHandler.onConnect(this, mSerializer);
                break;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return mSocket != null && !mSocket.isClosed() && mSocket.isConnected();
    }

    @Override
    public void close() throws Exception {
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
        }
    }

    @Override
    public void abort() throws Exception {
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
        }
    }

    @Override
    public void setOptions(TransportOptions options) {

    }
}
