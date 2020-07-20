package de.mkammerer.mrcanary.netty;

import de.mkammerer.mrcanary.canary.Canary;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;

@Slf4j
@RequiredArgsConstructor
public class FrontendHandler extends ChannelInboundHandlerAdapter {
    private final long id;
    private final Canary canary;

    @Nullable
    private Channel backendChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.trace("[{}] Frontend connected", id);

        ChannelFuture backendConnect = connectToBackend(ctx);
        backendChannel = backendConnect.channel();

        backendConnect.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.trace("[{}] Connected to backend", id);
                // Start reading from frontend, triggers channelRead()
                ctx.channel().read();
            } else {
                LOGGER.warn("[{}] Failed to connect to backend", id, future.cause());
                // Close connection to frontend
                ctx.channel().close();
            }
        });
    }

    private ChannelFuture connectToBackend(ChannelHandlerContext ctx) {
        InetSocketAddress backend = canary.getBackend();

        LOGGER.trace("[{}] Connecting to backend {}", id, backend);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
            .channel(ctx.channel().getClass())
            .handler(new BackendHandler(id, ctx.channel()))
            .option(ChannelOption.AUTO_READ, false);

        return bootstrap.connect(backend);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        // We must have a backend channel, as we only start reading after we have connected to the backend
        assert backendChannel != null;

        if (backendChannel.isActive()) {
            // Write data from frontend to backend
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("[{}] Proxying {} bytes from frontend to backend", id, NettyHelper.bufferSize(msg));
            }

            backendChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // Read more from frontend, triggers channelRead() again
                    ctx.channel().read();
                } else {
                    LOGGER.warn("[{}] Failed to write to backend", id, future.cause());
                    future.channel().close();
                }
            });
        } else {
            LOGGER.warn("[{}] Backend channel isn't active!", id);
            NettyHelper.flushAndClose(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (backendChannel != null) {
            LOGGER.trace("[{}] Disconnecting backend", id);
            NettyHelper.flushAndClose(backendChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("[{}] Unhandled exception", id, cause);
        NettyHelper.flushAndClose(ctx.channel());
    }
}
