package de.mkammerer.mrcanary.configuration;

import de.mkammerer.mrcanary.canary.CanaryId;
import de.mkammerer.mrcanary.canary.Color;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
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

    public InetSocketAddress getAddress(Color color) {
        switch (color) {
            case BLUE:
                return blueAddress;
            case GREEN:
                return greenAddress;
            case SHIFTING:
                throw new IllegalArgumentException("Can't get address for color " + Color.SHIFTING);
            default:
                throw new IllegalStateException("Unexpected value: " + color);
        }
    }

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
         * URI of the prometheus which holds the blue metrics.
         */
        URI blueUri;
        /**
         * URI of the prometheus which holds the green metrics.
         */
        URI greenUri;
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
        @Nullable Double min;
        /**
         * Query result maximum value.
         */
        @Nullable Double max;

        public URI getUriForColor(Color color) {
            switch (color) {
                case BLUE:
                    return blueUri;
                case GREEN:
                    return greenUri;
                case SHIFTING:
                    throw new IllegalArgumentException("Can't get uri for color " + Color.SHIFTING);
                default:
                    throw new IllegalStateException("Unexpected value: " + color);
            }
        }

        public String getQueryForColor(Color color) {
            switch (color) {
                case BLUE:
                    return blueQuery;
                case GREEN:
                    return greenQuery;
                case SHIFTING:
                    throw new IllegalArgumentException("Can't get query for color " + Color.SHIFTING);
                default:
                    throw new IllegalStateException("Unexpected value: " + color);
            }
        }
    }
}
