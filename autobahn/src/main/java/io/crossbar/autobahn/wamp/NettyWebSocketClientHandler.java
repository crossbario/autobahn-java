package io.crossbar.autobahn.wamp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.types.Welcome;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker mHandshaker;
    private final NettyTransport mTransport;
    private final ObjectMapper mSerializer;
    private ChannelPromise mHandshakeFuture;
    private ITransportHandler mTransportHandler;

    public NettyWebSocketClientHandler(WebSocketClientHandshaker handshaker, NettyTransport transport,
                                       ITransportHandler transportHandler, ObjectMapper serializer) {
        mHandshaker = handshaker;
        mTransport = transport;
        mTransportHandler = transportHandler;
        mSerializer = serializer;
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
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!mHandshaker.isHandshakeComplete()) {
            mHandshaker.finishHandshake(ch, (FullHttpResponse) msg);
            mHandshakeFuture.setSuccess();
            mTransportHandler.onOpen(mTransport);
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            byte[] output = new byte[binaryWebSocketFrame.content().readableBytes()];
            binaryWebSocketFrame.content().readBytes(output);
            List<Object> message = mSerializer.readValue(output, List.class);
            int type = (int) message.get(0);
            if (type == Welcome.MESSAGE_TYPE) {
                Welcome welcome = Welcome.parse(message);
                mTransportHandler.onMessage(welcome);
            }
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
            // FIXME: determine wasClean from CloseWebSocketFrame.statusCode()
            mTransportHandler.onClose(true);
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
}
