package de.mkammerer.mrcanary.netty.admin;

import com.google.gson.Gson;
import de.mkammerer.mrcanary.netty.NettyHelper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class AdminHandler extends ChannelInboundHandlerAdapter {
    private final Gson gson = new Gson();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            handle(ctx, (HttpRequest) msg);
        }
    }

    private void handle(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse response;

        switch (request.uri()) {
            case "/status":
                response = json(handleStatus());
                break;
            default:
                response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.NOT_FOUND);
                break;
        }

        ctx.write(response);
        NettyHelper.flushAndClose(ctx.channel());
    }

    private FullHttpResponse json(Object message) {
        String json = gson.toJson(message);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_0, HttpResponseStatus.OK, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return response;
    }

    private Object handleStatus() {
        return Map.of("status", "ok");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Unhandled exception", cause);
        NettyHelper.flushAndClose(ctx.channel());
    }
}
