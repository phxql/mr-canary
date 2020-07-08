package de.mkammerer.mrcanary;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ChildChannelInitializer extends ChannelInitializer<SocketChannel> {
    private long counter;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("Handler " + counter, new ProxyServerHandler());
        counter++;
    }
}
