package de.mkammerer.mrcanary.netty.admin.route;

import de.mkammerer.mrcanary.canary.CanaryManager;
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
    public Object execute(FullHttpRequest request) {
        if (request.method() != HttpMethod.GET) {
            return null;
        }

        return CanariesDto.of(
            Lists.map(canaryManager.getCanaries(), CanariesDto.CanaryDto::fromCanary)
        );
    }
}
