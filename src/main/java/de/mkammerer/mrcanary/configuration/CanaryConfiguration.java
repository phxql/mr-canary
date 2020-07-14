package de.mkammerer.mrcanary.configuration;

import de.mkammerer.mrcanary.canary.CanaryId;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;

@Value
public class CanaryConfiguration {
    /**
     * Name of the canary.
     */
    CanaryId id;
    /**
     * Port where the primary/canary can be reached.
     */
    int port;
    /**
     * Address of the blue system.
     */
    InetSocketAddress blueAddress;
    /**
     * Address of the green system.
     */
    InetSocketAddress greenAddress;
    /**
     * Prometheus configuration.
     */
    PrometheusConfiguration prometheus;
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
    WeightConfiguration weight;

    @Value
    public static class WeightConfiguration {
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
    public static class PrometheusConfiguration {
        /**
         * Query for the blue system to send to prometheus.
         */
        String blueQuery;
        /**
         * Query for the green system to send to prometheus.
         */
        String greenQuery;
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
