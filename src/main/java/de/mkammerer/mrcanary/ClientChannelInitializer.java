package de.mkammerer.mrcanary;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
    private long counter;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // TODO: Make address configurable
        ch.pipeline().addLast("Handler " + counter, new ClientChannelHandler(counter, new InetSocketAddress("www.mkammerer.de", 443)));
        counter++;
    }
}
