package de.mkammerer.mrcanary.netty.admin.route;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Value;

import java.util.Map;

@Value(staticConstructor = "of")
public class RouteResult {
    HttpResponseStatus status;
    Object body;

    public static RouteResult ok(Object body) {
        return new RouteResult(HttpResponseStatus.OK, body);
    }

    public static RouteResult methodNotAllowed(HttpMethod method, String uri) {
        return new RouteResult(HttpResponseStatus.METHOD_NOT_ALLOWED, Map.of(
            "method", method.toString(),
            "uri", uri
        ));
    }

    public static RouteResult notFound(String uri) {
        return new RouteResult(HttpResponseStatus.NOT_FOUND, Map.of(
            "uri", uri
        ));
    }

    public static RouteResult badRequest(String message) {
        return new RouteResult(HttpResponseStatus.BAD_REQUEST, Map.of(
            "message", message
        ));
    }
}
