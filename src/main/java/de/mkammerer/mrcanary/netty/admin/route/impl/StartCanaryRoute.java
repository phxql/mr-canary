package de.mkammerer.mrcanary.netty.admin.route.impl;

import de.mkammerer.mrcanary.canary.Canary;
import de.mkammerer.mrcanary.canary.CanaryId;
import de.mkammerer.mrcanary.canary.CanaryManager;
import de.mkammerer.mrcanary.canary.state.CanaryState;
import de.mkammerer.mrcanary.netty.admin.route.QueryString;
import de.mkammerer.mrcanary.netty.admin.route.Route;
import de.mkammerer.mrcanary.netty.admin.route.RouteResult;
import de.mkammerer.mrcanary.netty.admin.route.dto.StartCanaryDto;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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

        // Look up canary by id (return 404 if canary doesn't exist)
        Canary canary = canaryManager.getCanaryById(canaryId);
        if (canary == null) {
            return RouteResult.of(HttpResponseStatus.NOT_FOUND, Map.of(
                "message", String.format("Canary '%s' not found", canaryId)
            ));
        }

        if (canary.isRunning()) {
            return RouteResult.badRequest(String.format("Canary '%s' is already running", canaryId));
        }

        LOGGER.info("Starting rollout for canary '{}'", canaryId);
        CanaryState newState = canary.start();

        return RouteResult.ok(new StartCanaryDto(
            canaryId.getId(), newState.getStatus().toString(), newState.getWeight()
        ));
    }
}
