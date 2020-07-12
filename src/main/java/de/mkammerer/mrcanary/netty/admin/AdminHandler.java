package de.mkammerer.mrcanary.netty.admin;

import de.mkammerer.mrcanary.netty.NettyHelper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            handle(ctx, (HttpRequest) msg);
        }
    }

    private void handle(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_0, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER
        );
        ctx.write(response);
        NettyHelper.flushAndClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Unhandled exception", cause);
        NettyHelper.flushAndClose(ctx.channel());
    }
}
