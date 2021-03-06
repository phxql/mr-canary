package de.mkammerer.mrcanary.netty;

import de.mkammerer.mrcanary.canary.Canary;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Slf4j
public class ReverseProxyInitializer extends ChannelInitializer<SocketChannel> {
    private final Canary canary;

    // Gets incremented for every new connection
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public void initChannel(SocketChannel ch) {
        long id = idGenerator.getAndIncrement();

        LOGGER.trace("[{}] Initializing channel", id);
        ch.pipeline().addLast(new FrontendHandler(id, canary));
    }
}
