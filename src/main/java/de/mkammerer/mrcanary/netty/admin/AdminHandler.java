package de.mkammerer.mrcanary.netty.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mkammerer.mrcanary.netty.NettyHelper;
import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import de.mkammerer.mrcanary.netty.admin.route.impl.AbortCanaryRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.CanariesRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StartCanaryRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StatusRoute;
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
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class AdminHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Routes routes;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        LOGGER.trace("Handling request {} {}", msg.method(), msg.uri());
        FullHttpResponse response = handle(msg);
        LOGGER.trace("Response: {}", response.status());

        response.headers()
            .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    private FullHttpResponse handle(FullHttpRequest request) {
        QueryString queryString = new QueryString(new QueryStringDecoder(request.uri()));

        switch (queryString.getPath()) {
            case CanariesRoute.PATH:
                return execRoute(routes.getCanariesRoute(), request, queryString);
            case StatusRoute.PATH:
                return execRoute(routes.getStatusRoute(), request, queryString);
            case StartCanaryRoute.PATH:
                return execRoute(routes.getStartCanaryRoute(), request, queryString);
            case AbortCanaryRoute.PATH:
                return execRoute(routes.getAbortCanaryRoute(), request, queryString);
            default:
                return execRoute(routes.getDefaultRoute(), request, queryString);
        }
    }

    private FullHttpResponse execRoute(Route route, FullHttpRequest request, QueryString queryString) {
        RouteResult result = route.execute(request, queryString);
        return jsonResponse(result.getStatus(), result.getBody());
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
