package de.mkammerer.mrcanary.prometheus.impl.netty;

import de.mkammerer.mrcanary.prometheus.impl.ResultParser;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PrometheusInitializer extends ChannelInitializer<SocketChannel> {
    private final ResultParser resultParser;
    private final URI uri;
    private final String query;
    private final CompletableFuture<Double> result;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
            .addLast(new HttpClientCodec())
            .addLast(new HttpObjectAggregator(4 * 1024))
            .addLast(new PrometheusHandler(resultParser, uri, query, result));
    }
}
