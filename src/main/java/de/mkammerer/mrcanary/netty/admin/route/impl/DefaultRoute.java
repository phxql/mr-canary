package de.mkammerer.mrcanary.netty.admin.route.impl;

import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import io.netty.handler.codec.http.FullHttpRequest;

public class DefaultRoute implements Route {
    @Override
    public RouteResult execute(FullHttpRequest request, QueryString queryString) {
        return RouteResult.notFound(queryString.getUri());
    }
}
