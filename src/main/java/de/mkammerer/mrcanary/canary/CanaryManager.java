package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import de.mkammerer.mrcanary.util.Lists;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CanaryManager {
    /**
     * Known canaries. List is immutable!
     */
    private final Map<CanaryId, Canary> canaries;
    private final ScheduledExecutorService scheduler;

    private CanaryManager(List<Canary> canaries, ScheduledExecutorService scheduler) {
        this.canaries = groupCanaries(canaries);
        this.scheduler = scheduler;

        logCanaries();

        // TODO: Disable this!
        startCanaries();
    }

    private static Map<CanaryId, Canary> groupCanaries(List<Canary> canaries) {
        Map<CanaryId, Canary> result = new HashMap<>(canaries.size());
        for (Canary canary : canaries) {
            result.put(canary.getId(), canary);
        }
        return Collections.unmodifiableMap(result);
    }

    private void startCanaries() {
        for (Canary canary : canaries.values()) {
            canary.start(this);
        }
    }

    private void logCanaries() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Canary configuration:");
            for (Canary canary : canaries.values()) {
                LOGGER.info("  {} on port {} - blue backend: {}, green backend: {}", canary.getId(), canary.getPort(), canary.getBlueAddress(), canary.getGreenAddress());
            }
        }
    }

    /**
     * Is called by the scheduler when it's time to analyse the canary.
     *
     * @param canary canary to analyse
     */
    private void analyseCanary(Canary canary) {
        if (!canary.isRunning()) {
            return;
        }

        LOGGER.info("Analysing canary '{}'", canary.getId());
        canary.analyze();
    }

    public AnalysisJob scheduleAnalyzeJob(Canary canary) {
        Runnable runnable = () -> {
            try {
                analyseCanary(canary);
            } catch (RuntimeException e) {
                LOGGER.warn("Exception in scheduled job for canary '{}'", canary.getId(), e);
            }
        };
        long analysisIntervalInSeconds = canary.getAnalysisInterval().toSeconds();
        LOGGER.debug("Scheduling analysis job every {} for canary '{}'", canary.getAnalysisInterval(), canary.getId());
        ScheduledFuture<?> job = scheduler.scheduleWithFixedDelay(runnable, analysisIntervalInSeconds, analysisIntervalInSeconds, TimeUnit.SECONDS);

        // return the job to the caller so it can be stopped
        return () -> stopAnalyseJob(canary, job);
    }

    private void stopAnalyseJob(Canary canary, ScheduledFuture<?> job) {
        LOGGER.debug("Stopping analysis job for canary '{}'", canary.getId());
        job.cancel(true);
    }

    public Collection<Canary> getCanaries() {
        // List is immutable, no problem returning it here
        return canaries.values();
    }

    @Nullable
    public Canary getCanaryById(CanaryId id) {
        return canaries.get(id);
    }

    public static CanaryManager fromConfiguration(List<CanaryConfiguration> canaryConfigurations, ScheduledExecutorService scheduler, Prometheus prometheus, CanaryStateManager canaryStateManager) {
        return new CanaryManager(
            Lists.map(canaryConfigurations, configuration -> new Canary(configuration, prometheus, canaryStateManager)),
            scheduler
        );
    }
}
