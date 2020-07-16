package de.mkammerer.mrcanary.netty.admin;

import de.mkammerer.mrcanary.netty.admin.route.CanariesRoute;
import de.mkammerer.mrcanary.netty.admin.route.StatusRoute;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminInitializer extends ChannelInitializer<SocketChannel> {
    private final StatusRoute statusRoute;
    private final CanariesRoute canariesRoute;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast(new HttpServerCodec())
            .addLast(new HttpServerKeepAliveHandler())
            .addLast(new HttpObjectAggregator(4 * 1024))
            .addLast(new AdminHandler(statusRoute, canariesRoute));
    }
}
