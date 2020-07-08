package de.mkammerer.mrcanary;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
class ChildChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final AtomicLong counter = new AtomicLong();

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("Handler " + counter.getAndIncrement(), new ProxyServerHandler());
    }
}
