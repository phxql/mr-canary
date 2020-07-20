package de.mkammerer.mrcanary.prometheus.impl;

import de.mkammerer.mrcanary.prometheus.Prometheus;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class PrometheusMock implements Prometheus {
    private final long min;
    private final long max;

    @Override
    public CompletableFuture<Double> evaluate(URI uri, String query) {
        return CompletableFuture.completedFuture(ThreadLocalRandom.current().nextDouble(min, max + 1));
    }
}
