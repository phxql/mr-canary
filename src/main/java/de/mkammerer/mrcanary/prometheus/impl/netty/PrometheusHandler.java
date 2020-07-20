package de.mkammerer.mrcanary.prometheus.impl.netty;

import de.mkammerer.mrcanary.prometheus.impl.ResultParser;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
public class PrometheusHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final ResultParser resultParser;
    private final URI uri;
    private final String query;
    private final CompletableFuture<Double> result;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        LOGGER.debug("Connected to prometheus at {}, sending request", ctx.channel().remoteAddress());

        QueryStringEncoder queryEncoder = new QueryStringEncoder("/api/v1/query");
        queryEncoder.addParam("query", query);

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, queryEncoder.toString());
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());

        LOGGER.debug("Request: {} {}", request.method(), request.uri());

        ctx.channel().writeAndFlush(request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.debug("Sent request to {}", future.channel().remoteAddress());
            } else {
                result.completeExceptionally(future.cause());
            }
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        LOGGER.debug("Got response from {}", ctx.channel().remoteAddress());
        String body = msg.content().toString(StandardCharsets.UTF_8);
        LOGGER.trace("Body: {}", body);

        double prometheusResult = resultParser.parse(body);
        result.complete(prometheusResult);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        result.completeExceptionally(cause);
    }
}
