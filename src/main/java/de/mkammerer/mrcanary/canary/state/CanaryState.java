package de.mkammerer.mrcanary.canary.state;

import de.mkammerer.mrcanary.canary.Status;
import lombok.Value;
import lombok.With;

@Value
@With
public class CanaryState {
    Status status;
    int weight;
    int failures;

    public CanaryState incrementFailures() {
        return new CanaryState(status, weight, failures + 1);
    }

    public CanaryState increaseWeight(int delta) {
        return new CanaryState(status, weight + delta, failures);
    }
}
