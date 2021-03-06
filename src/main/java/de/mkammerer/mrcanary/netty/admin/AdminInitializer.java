package de.mkammerer.mrcanary.netty.admin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminInitializer extends ChannelInitializer<SocketChannel> {
    private final Routes routes;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast(new HttpServerCodec())
            .addLast(new HttpServerKeepAliveHandler())
            .addLast(new HttpObjectAggregator(4 * 1024))
            .addLast(new AdminHandler(routes));
    }
}
