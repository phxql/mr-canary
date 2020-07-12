package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.util.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CanaryManager {
    /**
     * Known canaries. List is immutable!
     */
    private final List<Canary> canaries;

    private CanaryManager(List<Canary> canaries) {
        this.canaries = List.copyOf(canaries);
        logCanaries();
    }

    private void logCanaries() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Canary configuration:");
            for (Canary canary : canaries) {
                LOGGER.info("  {} on port {} - primary backend: {}, canary backend: {}", canary.getName(), canary.getPort(), canary.getPrimaryAddress(), canary.getCanaryAddress());
            }
        }
    }

    public List<Canary> getCanaries() {
        return canaries;
    }

    public static CanaryManager fromConfiguration(List<CanaryConfiguration> canaryConfigurations) {
        return new CanaryManager(
            Lists.map(canaryConfigurations, Canary::new)
        );
    }
}
