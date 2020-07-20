package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.canary.state.CanaryState;
import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
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
    private final CanaryState defaultState;
    private final AtomicReference<@Nullable AnalysisJob> analyzeJob = new AtomicReference<>();

    public Canary(CanaryConfiguration configuration, Prometheus prometheus, CanaryStateManager canaryStateManager) {
        this.canaryId = configuration.getId();
        this.configuration = configuration;
        this.prometheus = prometheus;
        this.canaryStateManager = canaryStateManager;
        this.defaultState = new CanaryState(Status.INIT_BLUE, configuration.getWeight().getStart(), 0);

        initDefaultStateIfNeeded();
    }

    private void initDefaultStateIfNeeded() {
        CanaryState storedState = canaryStateManager.getState(canaryId);
        if (storedState == null) {
            LOGGER.debug("Initializing default canary state for canary '{}'", canaryId);
            setState(defaultState);
        } else {
            state.set(storedState);
        }
    }

    public InetSocketAddress getBackend() {
        CanaryState state = getState();

        if (!state.getStatus().isRunning()) {
            Color backendColor = state.getStatus().getBackendColor();
            return configuration.getAddress(backendColor);
        }

        Color primaryColor = state.getStatus().getPrimaryColor();
        Color canaryColor = state.getStatus().getCanaryColor();

        return castDice(state.getWeight(), configuration.getAddress(primaryColor), configuration.getAddress(canaryColor));
    }

    public CanaryState getState() {
        return state.get();
    }

    public boolean isRunning() {
        Status status = getState().getStatus();

        return status.isRunning();
    }

    public void analyze() {
        Status status = getState().getStatus();
        if (!status.isRunning()) {
            throw new IllegalStateException(String.format("Can't analyze, as canary '%s' isn't running", canaryId));
        }

        Color canaryColor = status.getCanaryColor();
        URI prometheusUri = configuration.getPrometheus().getUriForColor(canaryColor);
        String prometheusQuery = configuration.getPrometheus().getQueryForColor(canaryColor);

        LOGGER.debug("Querying {} prometheus for canary '{}'", canaryColor, canaryId);
        LOGGER.debug("Query: '{}'", prometheusQuery);
        CompletableFuture<Double> future = prometheus.evaluate(prometheusUri, prometheusQuery);
        future.whenComplete(this::handleAnalysisResult);
    }

    public CanaryState start(CanaryManager canaryManager) {
        CanaryState state = getState();

        if (state.getStatus().isRunning()) {
            throw new IllegalStateException(String.format("Canary '%s' is already running", canaryId));
        }

        Status newStatus = state.getStatus().getStartStatus();
        LOGGER.info("Starting canary '{}'. State changed: {} -> {}", canaryId, state.getStatus(), newStatus);

        CanaryState newState = defaultState.withStatus(newStatus);
        setState(newState);

        // Schedule analysis job
        this.analyzeJob.set(canaryManager.scheduleAnalyzeJob(this));

        return newState;
    }

    public CanaryState abort() {
        CanaryState state = getState();

        if (!state.getStatus().isRunning()) {
            throw new IllegalStateException(String.format("Canary '%s' isn't running", canaryId));
        }

        Status newStatus = state.getStatus().getAbortStatus();
        LOGGER.info("Aborting canary '{}'. State changed: {} -> {}", canaryId, state.getStatus(), newStatus);

        CanaryState newState = defaultState.withStatus(newStatus);
        setState(newState);

        stopAnalysisJob();

        return newState;
    }

    private void stopAnalysisJob() {
        AnalysisJob job = this.analyzeJob.get();
        assert job != null;
        job.stop();
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

    private void handleAnalysisResult(@Nullable Double result, @Nullable Throwable throwable) {
        if (throwable != null) {
            LOGGER.warn("Failed to query prometheus for canary '{}'", canaryId, throwable);
            analysisFailed();
            return;
        }

        assert result != null;
        LOGGER.debug("Prometheus result: {} for canary '{}'", result, canaryId);

        Double min = configuration.getPrometheus().getMin();
        Double max = configuration.getPrometheus().getMax();
        boolean success = true;

        if (min != null && result < min) {
            LOGGER.debug("Recording failure, as {} < {}", result, min);
            success = false;
        }
        if (max != null && result > max) {
            LOGGER.debug("Recording failure, as {} > {}", result, max);
            success = false;
        }

        if (success) {
            analysisSucceeded();
        } else {
            analysisFailed();
        }
    }

    private void analysisFailed() {
        LOGGER.info("Analysis result of '{}': failed", canaryId);

        CanaryState state = getState();
        state = state.incrementFailures();

        if (state.getFailures() > configuration.getMaxFailures()) {
            state = canaryFailed(state);
        }

        setState(state);
    }

    private void analysisSucceeded() {
        LOGGER.info("Analysis result of '{}': success", canaryId);

        CanaryState state = getState();
        state = state.increaseWeight(configuration.getWeight().getIncrease());

        if (state.getWeight() > configuration.getWeight().getEnd()) {
            state = canarySucceeded(state);
        } else {
            LOGGER.info("Routing {}% of traffic to {} for canary '{}'", state.getWeight(), state.getStatus().getCanaryColor(), canaryId);
        }

        setState(state);
    }

    private CanaryState canarySucceeded(CanaryState state) {
        CanaryState newState = state.withStatus(state.getStatus().getSuccessStatus()).withWeight(configuration.getWeight().getEnd());

        LOGGER.info("Canary '{}' success. Routing all traffic to {} from now on", canaryId, newState.getStatus().getBackendColor());
        stopAnalysisJob();
        return newState;
    }

    private CanaryState canaryFailed(CanaryState state) {
        CanaryState newState = state.withStatus(state.getStatus().getFailureStatus());

        LOGGER.info("Canary '{}' failed. Routing all traffic to {} from now on", canaryId, newState.getStatus().getBackendColor());
        stopAnalysisJob();
        return newState;
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
