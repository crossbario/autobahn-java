package io.crossbar.autobahn.wamp;

import java.util.List;

import io.crossbar.autobahn.wamp.interfaces.IMessage;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
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

import static io.crossbar.autobahn.wamp.messages.MessageMap.MESSAGE_TYPE_MAP;

public class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker mHandshaker;
    private final ITransport mTransport;
    private final ISerializer mSerializer;
    private ChannelPromise mHandshakeFuture;
    private ITransportHandler mTransportHandler;

    public NettyWebSocketClientHandler(WebSocketClientHandshaker handshaker, ITransport transport,
                                       ITransportHandler transportHandler, ISerializer serializer) {
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
            mTransportHandler.onConnect(mTransport);
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
            List<Object> message = mSerializer.unserialize(output, true);
            System.out.println(message);
            mTransportHandler.onMessage(getMessageObject(message));
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
        }
    }

    private IMessage getMessageObject(List<Object> rawMessage) throws Exception {
        int messageType = (int) rawMessage.get(0);
        Class<? extends IMessage> messageKlass = MESSAGE_TYPE_MAP.get(messageType);
        return (IMessage) messageKlass.getMethod("parse", List.class).invoke(null, rawMessage);
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
