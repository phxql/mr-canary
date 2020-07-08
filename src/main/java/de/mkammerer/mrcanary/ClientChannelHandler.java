package de.mkammerer.mrcanary;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
class ClientChannelHandler extends ChannelInboundHandlerAdapter {
    private final long id;
    private final SocketAddress upstreamAddress;
    private final List<Object> bufferedMessages = new ArrayList<>();

    private ChannelFuture upstreamConnect;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{}: Client connected", ctx.name());

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
            .channel(ctx.channel().getClass())
            .handler(new UpstreamChannelInitializer(id, ctx))
            .option(ChannelOption.AUTO_READ, false);
        upstreamConnect = bootstrap.connect(upstreamAddress);
        upstreamConnect.addListener((ChannelFutureListener) future -> {
            LOGGER.debug("{}: Upstream connected", ctx.name());
            flushBufferedDataIfNeeded(ctx);
        });
        upstreamConnect.channel().closeFuture().addListener((ChannelFutureListener) future -> {
            LOGGER.debug("{}: Upstream disconnected", ctx.name());
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!upstreamConnect.isDone()) {
            LOGGER.debug("{}: Connect to upstream isn't done, buffering message {}", ctx.name(), msg);
            bufferedMessages.add(msg);
        } else {
            flushBufferedDataIfNeeded(ctx);
            LOGGER.debug("{}: Sending {} to upstream", ctx.name(), msg);
            sendToUpstream(ctx, msg);
        }

    }

    private void flushBufferedDataIfNeeded(ChannelHandlerContext ctx) {
        if (bufferedMessages.isEmpty()) {
            return;
        }

        LOGGER.debug("{}: Writing {} buffered message(s) to upstream", ctx.name(), bufferedMessages.size());

        for (Object bufferedMessage : bufferedMessages) {
            sendToUpstream(ctx, bufferedMessage);
        }
        bufferedMessages.clear();
    }

    private void sendToUpstream(ChannelHandlerContext ctx, Object msg) {
        upstreamConnect.channel().writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.debug("{}: Wrote data to upstream", ctx.name());
            } else {
                LOGGER.warn("{}: Failed to write data to upstream", ctx.name(), future.cause());
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("{}: Client disconnected", ctx.name());

        if (upstreamConnect.isCancelled()) {
            LOGGER.debug("{}: Cancelling upstream connect", ctx.name());
            upstreamConnect.cancel(true);
        }

        if (upstreamConnect.channel().isOpen()) {
            LOGGER.debug("{}: Disconnect upstream", ctx.name());
            upstreamConnect.channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warn("{}: Exception while handling client connection", ctx.name(), cause);
        ctx.close();
    }
}
