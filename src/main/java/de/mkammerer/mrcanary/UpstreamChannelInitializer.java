package de.mkammerer.mrcanary;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class UpstreamChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final long id;
    private final ChannelHandlerContext ctx;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("Upstream " + id, new UpstreamChannelHandler(ctx.channel()));
    }
}
