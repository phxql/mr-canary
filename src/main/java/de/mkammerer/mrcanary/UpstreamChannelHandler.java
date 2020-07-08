package de.mkammerer.mrcanary;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
class UpstreamChannelHandler extends ChannelInboundHandlerAdapter {
    private final Channel in;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{}: Upstream connected", ctx.name());
        ctx.channel().read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.debug("{}: Received from upstream: {}", ctx.name(), msg);

        in.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.debug("{}: Wrote data to client", ctx.name());
                ctx.channel().read();
            } else {
                LOGGER.warn("{}: Failed to write data to client", ctx.name(), future.cause());
                future.channel().close();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warn("{}: Exception while handling upstream connection", ctx.name(), cause);
        ctx.close();
    }
}
