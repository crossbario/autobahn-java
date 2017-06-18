package io.crossbar.autobahn.wamp;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.types.CBORSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class NettyTransport implements ITransport {

    private Channel mChannel;
    private ISerializer mSerializer;
    private final String mUri;

    public NettyTransport(String uri) {
        mUri = uri;
        mSerializer = new CBORSerializer();
    }

    private int validateURIAndGetPort(URI uri) {
        String scheme = uri.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
        }
        int port = uri.getPort();
        if (port == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        return port;
    }

    private SslContext getSSLContext(String scheme) throws SSLException {
        if ("wss".equalsIgnoreCase(scheme)) {
            return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
        return null;
    }

    @Override
    public void connect(ITransportHandler transportHandler) {
        URI uri;
        try {
            uri = new URI(mUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        int port = validateURIAndGetPort(uri);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        final SslContext sslContext;
        try {
            sslContext = getSSLContext(scheme);
        } catch (SSLException e) {
            e.printStackTrace();
            return;
        }

        final NettyWebSocketClientHandler handler = new NettyWebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, "wamp.2.cbor",true,
                        new DefaultHttpHeaders(), 655360),
                this, transportHandler, mSerializer);

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline channelPipeline = ch.pipeline();
                if (sslContext != null) {
                    channelPipeline.addLast(sslContext.newHandler(ch.alloc(), host, port));
                }
                channelPipeline.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        WebSocketClientCompressionHandler.INSTANCE,
                        handler);
            }
        });

        try {
            mChannel = bootstrap.connect(uri.getHost(), port).sync().channel();
            handler.getHandshakeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(IMessage message) {
        byte[] data = mSerializer.serialize(message.marshal());
        WebSocketFrame frame = new BinaryWebSocketFrame(toByteBuf(data));
        mChannel.writeAndFlush(frame);
    }

    @Override
    public boolean isOpen() {
        return mChannel.isOpen();
    }

    @Override
    public void close() {
        System.out.println("ITransport.close()");
        try {
            mChannel.close().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void abort() {
        System.out.println("ITransport.abort()");
        close();
    }

    private ByteBuf toByteBuf(byte[] bytes) {
        return Unpooled.copiedBuffer(bytes);
    }
}
