package de.mkammerer.mrcanary.netty.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mkammerer.mrcanary.netty.NettyHelper;
import de.mkammerer.mrcanary.netty.admin.route.CanariesRoute;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.StatusRoute;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AdminHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final StatusRoute statusRoute;
    private final CanariesRoute canariesRoute;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        LOGGER.debug("Handling request {} {}", msg.method(), msg.uri());
        FullHttpResponse response = handle(msg);
        LOGGER.debug("Response: {}", response.status());

        response.headers()
            .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    private FullHttpResponse handle(FullHttpRequest request) {
        switch (request.uri()) {
            case CanariesRoute.PATH:
                return execRoute(canariesRoute, request);
            case StatusRoute.PATH:
                return execRoute(statusRoute, request);
            default:
                return jsonResponse(HttpResponseStatus.NOT_FOUND, Map.of("uri", request.uri()));
        }
    }

    private FullHttpResponse execRoute(Route route, FullHttpRequest request) {
        // Execute route
        Object body = route.execute(request);

        if (body == null) {
            return jsonResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, Map.of(
                "uri", request.uri(),
                "method", request.method().toString()
            ));
        }

        return jsonResponse(HttpResponseStatus.OK, body);
    }

    private DefaultFullHttpResponse jsonResponse(HttpResponseStatus status, Object body) {
        String json = gson.toJson(body);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Unhandled exception", cause);
        NettyHelper.flushAndClose(ctx.channel());
    }
}
