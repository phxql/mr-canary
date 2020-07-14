package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.canary.state.CanaryState;
import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class Canary {
    private final CanaryId canaryId;
    private final CanaryConfiguration configuration;
    private final Prometheus prometheus;
    private final CanaryStateManager canaryStateManager;

    public Canary(CanaryConfiguration configuration, Prometheus prometheus, CanaryStateManager canaryStateManager) {
        this.canaryId = configuration.getId();
        this.configuration = configuration;
        this.prometheus = prometheus;
        this.canaryStateManager = canaryStateManager;

        initDefaultStateIfNeeded();
    }

    private void initDefaultStateIfNeeded() {
        if (canaryStateManager.getState(canaryId) == null) {
            LOGGER.debug("Initializing default canary state for canary '{}'", canaryId);
            canaryStateManager.setState(canaryId, new CanaryState(Status.INIT_BLUE, configuration.getWeight().getStart(), 0));
        }
    }

    public InetSocketAddress getBackend() {
        CanaryState state = getState();

        switch (state.getStatus()) {
            case INIT_BLUE:
                return configuration.getBlueAddress();
            case INIT_GREEN:
                return configuration.getGreenAddress();
            case SHIFT_TO_BLUE:
                return castDice(state.getWeight(), configuration.getGreenAddress(), configuration.getBlueAddress());
            case SHIFT_TO_GREEN:
                return castDice(state.getWeight(), configuration.getBlueAddress(), configuration.getGreenAddress());
            case SUCCESS_BLUE:
                return configuration.getBlueAddress();
            case SUCCESS_GREEN:
                return configuration.getGreenAddress();
            case FAILED_BLUE:
                return configuration.getBlueAddress();
            case FAILED_GREEN:
                return configuration.getGreenAddress();
            default:
                throw new IllegalStateException("Unexpected value: " + state.getStatus());
        }
    }

    private CanaryState getState() {
        CanaryState state = canaryStateManager.getState(canaryId);
        if (state == null) {
            throw new IllegalStateException(String.format("Canary state is null for canary '%s'", canaryId));
        }
        return state;
    }

    private InetSocketAddress castDice(int weight, InetSocketAddress primary, InetSocketAddress canary) {
        // Range from 1 to 100 (both inclusive)
        int dice = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        // Current weight range from 0 to 100
        // if currentWeight == 0 always false
        // if currentWeight == 100 always true
        if (dice <= weight) {
            return canary;
        }

        return primary;
    }

    public boolean needAnalyze() {
        Status state = getState().getStatus();

        return state == Status.SHIFT_TO_BLUE || state == Status.SHIFT_TO_GREEN;
    }

    public void analyze() {
        Status status = getState().getStatus();
        String query;
        if (status == Status.SHIFT_TO_BLUE) {
            query = configuration.getPrometheus().getBlueQuery();
        } else if (status == Status.SHIFT_TO_GREEN) {
            query = configuration.getPrometheus().getGreenQuery();
        } else {
            throw new IllegalStateException(String.format("analyze() called despite status is %s", status));
        }

        LOGGER.debug("Querying prometheus for canary '{}'", canaryId);
        LOGGER.debug("Query: '{}'", query);
        CompletableFuture<Long> future = prometheus.evaluate(query);
        future.whenComplete(this::handleAnalysisResult);
    }

    private void handleAnalysisResult(@Nullable Long result, @Nullable Throwable throwable) {
        if (throwable != null) {
            LOGGER.warn("Failed to query prometheus for canary '{}'", canaryId, throwable);
            failure();
            return;
        }

        assert result != null;
        LOGGER.debug("Prometheus result: {} for canary '{}'", result, canaryId);

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
        LOGGER.info("Analysis result of '{}': failed", canaryId);

        CanaryState state = getState();
        state = state.incrementFailures();

        if (state.getFailures() > configuration.getMaxFailures()) {
            LOGGER.info("Canary '{}' failed. Routing all traffic to primary from now on", canaryId);

            if (state.getStatus() == Status.SHIFT_TO_BLUE) {
                state = state.withStatus(Status.FAILED_GREEN);
            } else if (state.getStatus() == Status.SHIFT_TO_GREEN) {
                state = state.withStatus(Status.FAILED_BLUE);
            } else {
                throw new IllegalStateException(String.format("Unexpected status while handling canary failure: %s", state.getStatus()));
            }
        }

        canaryStateManager.setState(canaryId, state);
    }

    private void success() {
        LOGGER.info("Analysis result of '{}': success", canaryId);

        CanaryState state = getState();
        state = state.increaseWeight(configuration.getWeight().getIncrease());

        if (state.getWeight() > configuration.getWeight().getEnd()) {
            LOGGER.info("Canary '{}' success. Routing all traffic to canary from now on", canaryId);
            if (state.getStatus() == Status.SHIFT_TO_BLUE) {
                state = state.withStatus(Status.SUCCESS_BLUE);
            } else if (state.getStatus() == Status.SHIFT_TO_GREEN) {
                state = state.withStatus(Status.SUCCESS_GREEN);
            } else {
                throw new IllegalStateException(String.format("Unexpected status while handling canary failure: %s", state.getStatus()));
            }
        } else {
            LOGGER.info("Routing {}% of traffic to canary '{}'", state.getWeight(), canaryId);
        }

        canaryStateManager.setState(canaryId, state);
    }

    public CanaryId getId() {
        return canaryId;
    }

    public int getPort() {
        return configuration.getPort();
    }

    public InetSocketAddress getBlueAddress() {
        return configuration.getBlueAddress();
    }

    public InetSocketAddress getGreenAddress() {
        return configuration.getGreenAddress();
    }

    public Duration getAnalysisInterval() {
        return configuration.getAnalysisInterval();
    }
}
