package de.mkammerer.mrcanary.canary;

import de.mkammerer.mrcanary.configuration.CanaryConfiguration;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Canary {
    private final AtomicReference<Status> status = new AtomicReference<>(Status.RUNNING);
    /**
     * Percentage of traffic going to the canary.
     */
    private final AtomicInteger canaryWeight = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);

    private final CanaryConfiguration configuration;

    public Canary(CanaryConfiguration configuration) {
        this.configuration = configuration;

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
}
