package de.mkammerer.mrcanary.netty.admin.route;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;

public class StatusRoute implements Route {
    public static final String PATH = "/status";

    @Override
    public Object execute(FullHttpRequest request) {
        if (request.method() != HttpMethod.GET) {
            return null;
        }

        return Map.of("status", "ok");
    }
}
