package de.mkammerer.mrcanary.configuration;

import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;

@Value
public class CanaryConfiguration {
    /**
     * Name of the canary.
     */
    String name;
    /**
     * Port where the primary/canary can be reached.
     */
    int port;
    /**
     * Address of the primary.
     */
    InetSocketAddress primaryAddress;
    /**
     * Address of the canary.
     */
    InetSocketAddress canaryAddress;
    /**
     * Prometheus configuration.
     */
    Prometheus prometheus;
    /**
     * Maximum number of failures abort the rollout.
     */
    int maxFailures;
    /**
     * Time to wait before weight increasing steps.
     */
    Duration analysisInterval;
    /**
     * Weight configuration.
     */
    Weight weight;

    @Value
    public static class Weight {
        /**
         * Starting weight percentage.
         */
        int start;
        /**
         * End weight percentage. If this is reached without too much failures, the canary is promoted.
         */
        int end;
        /**
         * Increase percentage.
         */
        int increase;
    }

    @Value
    public static class Prometheus {
        /**
         * Query to send to prometheus.
         */
        String query;
        /**
         * Query result minimum value.
         */
        @Nullable Long min;
        /**
         * Query result maximum value.
         */
        @Nullable Long max;
    }
}
