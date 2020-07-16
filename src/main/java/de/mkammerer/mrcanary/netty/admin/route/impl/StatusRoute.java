package de.mkammerer.mrcanary.netty.admin.route.impl;

import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;

public class StatusRoute implements Route {
    public static final String PATH = "/status";

    @Override
    public RouteResult execute(FullHttpRequest request, QueryString queryString) {
        if (request.method() != HttpMethod.GET) {
            return RouteResult.methodNotAllowed(request.method(), queryString.getUri());
        }

        return RouteResult.ok(
            Map.of("status", "ok")
        );
    }
}
