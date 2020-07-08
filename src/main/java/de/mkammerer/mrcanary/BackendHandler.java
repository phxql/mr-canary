package de.mkammerer.mrcanary;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BackendHandler extends ChannelInboundHandlerAdapter {
    private final long id;
    private final Channel frontendChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Start reading from backend, triggers channelRead()
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        // Write data from backend to frontend
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[{}] Proxying {} bytes from backend to frontend", id, Helper.bufferSize(msg));
        }

        frontendChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Read more from backend, triggers channelRead() again
                ctx.channel().read();
            } else {
                LOGGER.warn("[{}] Failed to write to frontend", id, future.cause());
                frontendChannel.close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.debug("[{}] Disconnecting frontend", id);
        Helper.flushAndClose(frontendChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("[{}] Unhandled exception", id, cause);
        Helper.flushAndClose(ctx.channel());
    }
}
