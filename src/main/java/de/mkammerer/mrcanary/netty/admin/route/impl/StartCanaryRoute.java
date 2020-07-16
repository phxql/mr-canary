package de.mkammerer.mrcanary.netty.admin.route.impl;

import de.mkammerer.mrcanary.canary.CanaryId;
import de.mkammerer.mrcanary.canary.CanaryManager;
import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class StartCanaryRoute implements Route {
    public static final String PATH = "/start";

    private final CanaryManager canaryManager;

    @Override
    public RouteResult execute(FullHttpRequest request, QueryString queryString) {
        if (request.method() != HttpMethod.POST) {
            return RouteResult.methodNotAllowed(request.method(), queryString.getUri());
        }

        String canaryIdParameter = queryString.getQueryParameter("canary");
        if (canaryIdParameter == null) {
            return RouteResult.badRequest("Missing query parameter 'canary'");
        }
        CanaryId canaryId = CanaryId.of(canaryIdParameter);
        LOGGER.info("Starting rollout for canary '{}'", canaryId);

        // TODO: Implement
        return RouteResult.ok(new Object());
    }
}
