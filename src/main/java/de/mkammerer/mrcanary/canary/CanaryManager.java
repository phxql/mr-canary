package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import de.mkammerer.mrcanary.util.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CanaryManager {
    /**
     * Known canaries. List is immutable!
     */
    private final List<Canary> canaries;
    private final ScheduledExecutorService scheduler;

    private CanaryManager(List<Canary> canaries, ScheduledExecutorService scheduler) {
        this.canaries = List.copyOf(canaries);
        this.scheduler = scheduler;

        logCanaries();
        registerAnalysisCallbacks();
    }

    private void logCanaries() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Canary configuration:");
            for (Canary canary : canaries) {
                LOGGER.info("  {} on port {} - blue backend: {}, green backend: {}", canary.getId(), canary.getPort(), canary.getBlueAddress(), canary.getGreenAddress());
            }
        }
    }

    private void registerAnalysisCallbacks() {
        for (Canary canary : canaries) {
            long analysisIntervalInSeconds = canary.getAnalysisInterval().toSeconds();

            scheduler.scheduleWithFixedDelay(() -> analyseCanary(canary), analysisIntervalInSeconds, analysisIntervalInSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * Is called by the scheduler when it's time to analyse the canary.
     *
     * @param canary canary to analyse
     */
    private void analyseCanary(Canary canary) {
        if (!canary.needAnalyze()) {
            return;
        }

        LOGGER.info("Analysing canary '{}'", canary.getId());
        canary.analyze();
    }

    public List<Canary> getCanaries() {
        // List is immutable, no problem returning it here
        return canaries;
    }

    public static CanaryManager fromConfiguration(List<CanaryConfiguration> canaryConfigurations, ScheduledExecutorService scheduler, Prometheus prometheus, CanaryStateManager canaryStateManager) {
        return new CanaryManager(
            Lists.map(canaryConfigurations, configuration -> new Canary(configuration, prometheus, canaryStateManager)),
            scheduler
        );
    }
}
