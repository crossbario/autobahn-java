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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.TransportOptions;
import io.crossbar.autobahn.wamp.types.WebSocketOptions;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;


public class NettyWebSocket implements ITransport {

    private static final IABLogger LOGGER = ABLogger.getLogger(
            NettyWebSocket.class.getName());
    private static final String SERIALIZERS_DEFAULT = String.format(
            "%s,%s,%s", CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME);

    private Channel mChannel;
    private NettyWebSocketClientHandler mHandler;
    private final String mUri;

    private WebSocketOptions mOptions;
    private String mSerializers;

    public NettyWebSocket(String uri) {
        this(uri, (WebSocketOptions) null);
    }

    public NettyWebSocket(String uri, List<String> serializers) {
        this(uri, serializers, null);
    }

    @Deprecated
    public NettyWebSocket(String uri, WebSocketOptions options) {
        this(uri, null, options);
    }

    @Deprecated
    public NettyWebSocket(String uri, List<String> serializers, WebSocketOptions options) {
        mUri = uri;

        if (serializers == null) {
            mSerializers = SERIALIZERS_DEFAULT;
        } else {
            StringBuilder result = new StringBuilder();
            for (String serializer: serializers) {
                result.append(serializer).append(",");
            }
            mSerializers = result.toString();
        }

        if (options == null) {
            mOptions = new WebSocketOptions();
        } else {
            mOptions = options;
        }
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
        return "wss".equalsIgnoreCase(scheme) ? SslContextBuilder.forClient().trustManager(
                InsecureTrustManagerFactory.INSTANCE).build() : null;
    }

    @Override
    public void connect(ITransportHandler transportHandler) throws Exception {
        connect(transportHandler, new TransportOptions());
    }

    @Override
    public void connect(ITransportHandler transportHandler, TransportOptions options)
            throws Exception {

        if (options == null) {
            if (mOptions == null) {
                options = new TransportOptions();
            } else {
                options = new TransportOptions();
                options.setAutoPingInterval(mOptions.getAutoPingInterval());
                options.setAutoPingTimeout(mOptions.getAutoPingTimeout());
                options.setMaxFramePayloadSize(mOptions.getMaxFramePayloadSize());
            }
        }

        URI uri;
        uri = new URI(mUri);
        int port = validateURIAndGetPort(uri);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        final SslContext sslContext = getSSLContext(scheme);

        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, mSerializers, true,
                new DefaultHttpHeaders(), options.getMaxFramePayloadSize());
        mHandler = new NettyWebSocketClientHandler(handshaker, this, transportHandler);

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);

        TransportOptions opt = options;
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
                        new IdleStateHandler(
                                opt.getAutoPingInterval() + opt.getAutoPingTimeout(),
                                opt.getAutoPingInterval(), 0, TimeUnit.SECONDS),
                        mHandler);
            }
        });
        ChannelFuture f = bootstrap.connect(uri.getHost(), port);
        f.addListener((ChannelFutureListener) connectFuture -> {
            Throwable connectCause = connectFuture.cause();
            if (connectCause != null) {
                transportHandler.onDisconnect(false);
            } else {
                mChannel = f.channel();
            }
        });
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
        return mChannel != null && mChannel.isOpen();
    }

    @Override
    public void close() throws Exception {
        LOGGER.v("close()");
        if (mHandler != null && mChannel != null) {
            mHandler.close(mChannel, true, new CloseDetails(CloseDetails.REASON_DEFAULT, null));
        }
    }

    @Override
    public void abort() throws Exception {
        LOGGER.v("abort()");
        close();
    }

    @Override
    public void setOptions(TransportOptions options) {
        throw new UnsupportedOperationException(
                "Not implemented yet, provide options using connect() instead");
    }

    private ByteBuf toByteBuf(byte[] bytes) {
        return Unpooled.copiedBuffer(bytes);
    }
}
