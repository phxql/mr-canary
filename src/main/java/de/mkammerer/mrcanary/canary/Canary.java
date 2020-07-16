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
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Canary {
    private final CanaryId canaryId;
    private final CanaryConfiguration configuration;
    private final Prometheus prometheus;
    private final CanaryStateManager canaryStateManager;
    private final AtomicReference<CanaryState> state = new AtomicReference<>();

    public Canary(CanaryConfiguration configuration, Prometheus prometheus, CanaryStateManager canaryStateManager) {
        this.canaryId = configuration.getId();
        this.configuration = configuration;
        this.prometheus = prometheus;
        this.canaryStateManager = canaryStateManager;

        initDefaultStateIfNeeded();
    }

    private void initDefaultStateIfNeeded() {
        CanaryState storeState = canaryStateManager.getState(canaryId);
        if (storeState == null) {
            LOGGER.debug("Initializing default canary state for canary '{}'", canaryId);
            setState(new CanaryState(Status.INIT_BLUE, configuration.getWeight().getStart(), 0));
        } else {
            state.set(storeState);
        }
    }

    public InetSocketAddress getBackend() {
        CanaryState state = getState();

        switch (state.getStatus().getBackendColor()) {
            case BLUE:
                return configuration.getBlueAddress();
            case GREEN:
                return configuration.getGreenAddress();
            case SHIFTING:
                if (state.getStatus() == Status.SHIFT_TO_BLUE) {
                    return castDice(state.getWeight(), configuration.getGreenAddress(), configuration.getBlueAddress());
                } else if (state.getStatus() == Status.SHIFT_TO_GREEN) {
                    return castDice(state.getWeight(), configuration.getBlueAddress(), configuration.getGreenAddress());
                } else {
                    throw new IllegalStateException("Unexpected value: " + state.getStatus());
                }

            default:
                throw new IllegalStateException("Unexpected value: " + state.getStatus().getBackendColor());
        }
    }

    public CanaryState getState() {
        return state.get();
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

        return state.getBackendColor() == Color.SHIFTING;
    }

    public void analyze() {
        Status status = getState().getStatus();
        Color canaryColor = getCanaryColor(status);
        String query = configuration.getPrometheus().getQueryForColor(canaryColor);

        LOGGER.debug("Querying {} prometheus for canary '{}'", canaryColor, canaryId);
        LOGGER.debug("Query: '{}'", query);
        CompletableFuture<Long> future = prometheus.evaluate(query);
        future.whenComplete(this::handleAnalysisResult);
    }

    private Color getCanaryColor(Status status) {
        switch (status) {
            case SHIFT_TO_BLUE:
                return Color.BLUE;
            case SHIFT_TO_GREEN:
                return Color.GREEN;
            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }
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
            if (state.getStatus() == Status.SHIFT_TO_BLUE) {
                // Failure while shifting to blue results in green backend used
                state = state.withStatus(Status.FAILED_GREEN);
            } else if (state.getStatus() == Status.SHIFT_TO_GREEN) {
                // Failure while shifting to green results in blue backend used
                state = state.withStatus(Status.FAILED_BLUE);
            } else {
                throw new IllegalStateException(String.format("Unexpected status while handling canary failure: %s", state.getStatus()));
            }

            LOGGER.info("Canary '{}' failed. Routing all traffic to {} from now on", canaryId, state.getStatus().getBackendColor());
        }

        setState(state);
    }

    private void success() {
        LOGGER.info("Analysis result of '{}': success", canaryId);

        CanaryState state = getState();
        state = state.increaseWeight(configuration.getWeight().getIncrease());

        if (state.getWeight() > configuration.getWeight().getEnd()) {
            if (state.getStatus() == Status.SHIFT_TO_BLUE) {
                // Success while shifting to blue results in blue backend used
                state = state.withStatus(Status.SUCCESS_BLUE);
            } else if (state.getStatus() == Status.SHIFT_TO_GREEN) {
                // Success while shifting to green results in green backend used
                state = state.withStatus(Status.SUCCESS_GREEN);
            } else {
                throw new IllegalStateException(String.format("Unexpected status while handling canary failure: %s", state.getStatus()));
            }

            LOGGER.info("Canary '{}' success. Routing all traffic to {} from now on", canaryId, state.getStatus().getBackendColor());
        } else {
            LOGGER.info("Routing {}% of traffic to {} for canary '{}'", state.getWeight(), getCanaryColor(state.getStatus()), canaryId);
        }

        setState(state);
    }

    private void setState(CanaryState newState) {
        state.set(newState);
        canaryStateManager.setState(canaryId, newState);
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
