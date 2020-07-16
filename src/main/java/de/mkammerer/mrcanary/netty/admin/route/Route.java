package de.mkammerer.mrcanary.netty.admin.route;

import io.netty.handler.codec.http.FullHttpRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Route {
    /**
     * Executes the route, returning the body as DTO. Returns null if HTTP method doesn't match.
     *
     * @param request request
     * @return body as DTO. null if HTTP method doesn't match
     */
    @Nullable
    Object execute(FullHttpRequest request);
}
