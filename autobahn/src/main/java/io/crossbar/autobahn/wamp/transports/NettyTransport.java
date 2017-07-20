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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.wamp.types.WebSocketOptions;
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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;


public class NettyTransport implements ITransport {

    private static final Logger LOGGER = Logger.getLogger(NettyTransport.class.getName());
    private static final String SERIALIZERS_DEFAULT = String.format(
            "%s,%s,%s", CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME);

    private Channel mChannel;
    private final String mUri;

    private ExecutorService mExecutor;
    private WebSocketOptions mOptions;
    private List<String> mSerializers;

    public NettyTransport(String uri) {
        mUri = uri;
    }

    public NettyTransport(String uri, List<String> serializers) {
        mUri = uri;
        mSerializers = serializers;
    }

    public NettyTransport(String uri, WebSocketOptions options) {
        this(uri);
        mOptions = options;
    }

    public NettyTransport(String uri, ExecutorService executor) {
        this(uri);
        mExecutor = executor;
    }

    public NettyTransport(String uri, List<String> serializers, ExecutorService executor) {
        this(uri);
        mExecutor = executor;
        mSerializers = serializers;
    }

    public NettyTransport(String uri, ExecutorService executor, WebSocketOptions options) {
        this(uri);
        mExecutor = executor;
        mOptions = options;
    }

    private ExecutorService getExecutor() {
        if (mExecutor == null) {
            mExecutor = ForkJoinPool.commonPool();
        }
        return mExecutor;
    }

    private WebSocketOptions getOptions() {
        if (mOptions == null) {
            mOptions = new WebSocketOptions();
        }
        return mOptions;
    }

    private int validateURIAndGetPort(URI uri) {
        String scheme = uri.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only WS(S) is supported.");
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

    private String getSerializers() {
        if (mSerializers != null) {
            StringBuilder result = new StringBuilder();
            mSerializers.forEach(s -> result.append(s).append(","));
            return result.toString();
        }
        return SERIALIZERS_DEFAULT;
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
                        uri, WebSocketVersion.V13, getSerializers(),true,
                        new DefaultHttpHeaders(), getOptions().getMaxFramePayloadSize()),
                this, transportHandler);

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
    public void send(byte[] payload, boolean isBinary) {
        WebSocketFrame frame;
        if (isBinary) {
            frame = new BinaryWebSocketFrame(toByteBuf(payload));
        } else {
            frame = new TextWebSocketFrame(toByteBuf(payload));
        }
        mChannel.writeAndFlush(frame);
    }

    @Override
    public boolean isOpen() {
        return mChannel.isOpen();
    }

    @Override
    public void close() {
        LOGGER.info("close()");
        try {
            mChannel.close().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void abort() {
        LOGGER.info("abort()");
        close();
    }

    private ByteBuf toByteBuf(byte[] bytes) {
        return Unpooled.copiedBuffer(bytes);
    }
}
