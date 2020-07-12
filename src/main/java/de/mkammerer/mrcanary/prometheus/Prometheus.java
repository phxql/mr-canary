package de.mkammerer.mrcanary.prometheus;

public interface Prometheus {
    long evaluate(String query);
}
