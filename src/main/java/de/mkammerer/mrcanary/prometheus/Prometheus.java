package de.mkammerer.mrcanary.prometheus;

import java.util.concurrent.CompletableFuture;

public interface Prometheus {
    CompletableFuture<Long> evaluate(String query);
}
