package de.mkammerer.mrcanary.canary.state;

import de.mkammerer.mrcanary.canary.CanaryId;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface CanaryStateManager {
    @Nullable
    CanaryState getState(CanaryId id);

    void setState(CanaryId id, CanaryState state);
}
