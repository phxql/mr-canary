package de.mkammerer.mrcanary.canary;

import lombok.Value;

@Value(staticConstructor = "of")
public class CanaryId {
    String id;

    @Override
    public String toString() {
        return id;
    }
}
