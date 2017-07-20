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

import java.util.logging.Logger;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.util.CharsetUtil;


public class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOGGER = Logger.getLogger(
            NettyWebSocketClientHandler.class.getName());

    private final WebSocketClientHandshaker mHandshaker;
    private final ITransport mTransport;
    private ChannelPromise mHandshakeFuture;
    private ITransportHandler mTransportHandler;

    public NettyWebSocketClientHandler(WebSocketClientHandshaker handshaker, ITransport transport,
                                       ITransportHandler transportHandler) {
        mHandshaker = handshaker;
        mTransport = transport;
        mTransportHandler = transportHandler;
    }

    public ChannelFuture getHandshakeFuture() {
        return mHandshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        mHandshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        mHandshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("WebSocket Client disconnected!");
        // FIXME: wasClean flag
        mTransportHandler.onDisconnect(true);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!mHandshaker.isHandshakeComplete()) {
            FullHttpResponse response = (FullHttpResponse) msg;
            String negotiatedSerializer = response.headers().get("Sec-WebSocket-Protocol");
            LOGGER.info(String.format("Negotiated serializer=%s", negotiatedSerializer));
            ISerializer serializer = initializeSerializer(negotiatedSerializer);
            mHandshaker.finishHandshake(ch, response);
            mHandshakeFuture.setSuccess();
            mTransportHandler.onConnect(mTransport, serializer);

        } else if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');

        } else if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) msg;
            byte[] payload = new byte[binaryWebSocketFrame.content().readableBytes()];
            LOGGER.info(String.format("Received binary frame, content length=%s", payload.length));
            binaryWebSocketFrame.content().readBytes(payload);
            mTransportHandler.onMessage(payload, true);

        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) msg;
            byte[] payload = new byte[textWebSocketFrame.content().readableBytes()];
            LOGGER.info(String.format("Received Text frame, content length=%s", payload.length));
            textWebSocketFrame.content().readBytes(payload);
            mTransportHandler.onMessage(payload, false);

        } else if (msg instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame textWebSocketFrame = (CloseWebSocketFrame) msg;
            LOGGER.info(String.format(
                    "Received Close frame, code=%s, reason=%s",
                    textWebSocketFrame.statusCode(), textWebSocketFrame.reasonText()));
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!mHandshakeFuture.isDone()) {
            mHandshakeFuture.setFailure(cause);
        }
        ctx.close();
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
