package de.mkammerer.mrcanary.prometheus.impl;

import de.mkammerer.mrcanary.prometheus.Prometheus;
import de.mkammerer.mrcanary.prometheus.impl.netty.PrometheusInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
public class NettyPrometheus implements Prometheus {
    private final EventLoopGroup eventLoopGroup;

    @Override
    public CompletableFuture<Long> evaluate(URI uri, String query) {
        CompletableFuture<Long> result = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
            .channel(NioSocketChannel.class)
            .handler(new PrometheusInitializer(uri, query, result));

        String host = uri.getHost();
        int port = getPortOrDefault(uri.getPort(), uri.getScheme());
        LOGGER.debug("Connecting to prometheus at {}:{}", host, port);
        bootstrap.connect(host, port);

        return result;
    }

    private int getPortOrDefault(int port, String scheme) {
        if (port != -1) {
            // Port is specified
            return port;
        }

        switch (scheme.toLowerCase()) {
            case "http":
                return 80;
            case "https":
                return 443;
            default:
                throw new IllegalStateException("Unexpected value: " + scheme);
        }
    }
}
