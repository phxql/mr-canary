package de.mkammerer.mrcanary.netty.admin.route;

import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

@RequiredArgsConstructor
public class QueryString {
    private final QueryStringDecoder decoder;

    public String getUri() {
        return decoder.uri();
    }

    public String getPath() {
        return decoder.path();
    }

    /**
     * Returns the value of the first query parameter with the given name.
     * <p>
     * Returns null if the parameter doesn't exist or has no value.
     *
     * @param name name
     * @return query parameter value. Can be null.
     */
    @Nullable
    public String getQueryParameter(String name) {
        List<String> values = decoder.parameters().get(name);

        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
