package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Canary {
    private final AtomicReference<Status> status = new AtomicReference<>(Status.RUNNING);
    /**
     * Percentage of traffic going to the canary.
     */
    private final AtomicInteger canaryWeight = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);

    private final CanaryConfiguration configuration;
    private final Prometheus prometheus;

    public Canary(CanaryConfiguration configuration, Prometheus prometheus) {
        this.configuration = configuration;
        this.prometheus = prometheus;

        // Initialize to start weight
        canaryWeight.set(configuration.getWeight().getStart());
    }

    public InetSocketAddress getBackend() {
        Status currentStatus = this.status.get();
        switch (currentStatus) {
            case RUNNING:
                return castDice();
            case FAILED:
                // Failed = all traffic to primary
                return configuration.getPrimaryAddress();
            case SUCCESS:
                // Success = all traffic to canary
                return configuration.getCanaryAddress();
            default:
                throw new IllegalStateException("Unexpected value: " + currentStatus);
        }
    }

    private InetSocketAddress castDice() {
        // Range from 1 to 100 (both inclusive)
        int dice = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        // Current weight range from 0 to 100
        // if currentWeight == 0 always false
        // if currentWeight == 100 always true
        if (dice <= canaryWeight.get()) {
            return configuration.getCanaryAddress();
        }

        return configuration.getPrimaryAddress();
    }

    public boolean needAnalyze() {
        return status.get() == Status.RUNNING;
    }

    public void analyze() {
        LOGGER.debug("Querying prometheus for canary '{}'", configuration.getName());
        CompletableFuture<Long> future = prometheus.evaluate(configuration.getPrometheus().getQuery());
        future.whenComplete(this::handleAnalysisResult);
    }

    private void handleAnalysisResult(@Nullable Long result, @Nullable Throwable throwable) {
        if (throwable != null) {
            LOGGER.warn("Failed to query prometheus for canary '{}'", configuration.getName(), throwable);
            failure();
            return;
        }

        assert result != null;
        LOGGER.debug("Prometheus result: {} for canary '{}'", result, configuration.getName());

        Long min = configuration.getPrometheus().getMin();
        Long max = configuration.getPrometheus().getMax();
        boolean success = true;

        if (min != null && result < min) {
            LOGGER.trace("Recording failure, as {} < {}", result, min);
            success = false;
        }
        if (max != null && result > max) {
            LOGGER.trace("Recording failure, as {} > {}", result, max);
            success = false;
        }

        if (success) {
            success();
        } else {
            failure();
        }
    }

    private void failure() {
        LOGGER.info("Analysis result of '{}': failed", configuration.getName());
        int currentFailures = failures.incrementAndGet();

        if (currentFailures > configuration.getMaxFailures()) {
            LOGGER.info("Canary '{}' failed. Routing all traffic to primary from now on", configuration.getName());
            status.set(Status.FAILED);
        }
    }

    private void success() {
        LOGGER.info("Analysis result of '{}': success", configuration.getName());

        int currentWeight = canaryWeight.addAndGet(configuration.getWeight().getIncrease());
        if (currentWeight > configuration.getWeight().getEnd()) {
            LOGGER.info("Canary '{}' success. Routing all traffic to canary from now on", configuration.getName());
            status.set(Status.SUCCESS);
        } else {
            LOGGER.info("Routing {}% of traffic to canary '{}'", currentWeight, configuration.getName());
        }
    }

    public String getName() {
        return configuration.getName();
    }

    public int getPort() {
        return configuration.getPort();
    }

    public InetSocketAddress getCanaryAddress() {
        return configuration.getCanaryAddress();
    }

    public InetSocketAddress getPrimaryAddress() {
        return configuration.getPrimaryAddress();
    }

    public Duration getAnalysisInterval() {
        return configuration.getAnalysisInterval();
    }
}
