package de.mkammerer.mrcanary;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Slf4j
public class ReverseProxyInitializer extends ChannelInitializer<SocketChannel> {
    private final SocketAddress backendAddress;

    // Gets incremented for every new connection
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public void initChannel(SocketChannel ch) {
        long id = idGenerator.getAndIncrement();

        LOGGER.debug("[{}] Initializing channel", id);
        ch.pipeline().addLast(new FrontendHandler(id, backendAddress));
    }
}
