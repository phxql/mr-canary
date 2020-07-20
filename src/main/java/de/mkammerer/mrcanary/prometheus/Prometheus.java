package de.mkammerer.mrcanary.prometheus;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface Prometheus {
    CompletableFuture<Double> evaluate(URI uri, String query);
}
