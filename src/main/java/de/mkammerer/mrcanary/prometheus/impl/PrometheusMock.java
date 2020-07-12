package de.mkammerer.mrcanary.prometheus.impl;

import de.mkammerer.mrcanary.prometheus.Prometheus;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class PrometheusMock implements Prometheus {
    private final long min;
    private final long max;

    @Override
    public long evaluate(String query) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
