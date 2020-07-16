package de.mkammerer.mrcanary.netty.admin.route.impl;

import de.mkammerer.mrcanary.canary.CanaryManager;
import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import de.mkammerer.mrcanary.netty.admin.route.dto.CanariesDto;
import de.mkammerer.mrcanary.util.Lists;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CanariesRoute implements Route {
    public static final String PATH = "/";

    private final CanaryManager canaryManager;

    @Override
    public RouteResult execute(FullHttpRequest request, QueryString queryString) {
        if (request.method() != HttpMethod.GET) {
            return RouteResult.methodNotAllowed(request.method(), queryString.getUri());
        }

        return RouteResult.ok(CanariesDto.of(
            Lists.map(canaryManager.getCanaries(), CanariesDto.CanaryDto::fromCanary)
        ));
    }
}
