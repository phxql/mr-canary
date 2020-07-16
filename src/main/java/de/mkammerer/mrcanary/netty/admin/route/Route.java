package de.mkammerer.mrcanary.netty.admin.route;

import io.netty.handler.codec.http.FullHttpRequest;

public interface Route {
    /**
     * Executes the route, returning the body as DTO.
     *
     * @param request     request
     * @param queryString decoded query string
     * @return body as DTO
     */
    RouteResult execute(FullHttpRequest request, QueryString queryString);
}
