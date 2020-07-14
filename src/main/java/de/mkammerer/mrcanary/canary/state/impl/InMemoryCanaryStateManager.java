package de.mkammerer.mrcanary.canary.state.impl;

import de.mkammerer.mrcanary.canary.CanaryId;
import de.mkammerer.mrcanary.canary.state.CanaryState;
import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores state in the local memory.
 */
public class InMemoryCanaryStateManager implements CanaryStateManager {
    private final ConcurrentMap<CanaryId, CanaryState> states = new ConcurrentHashMap<>();

    @Override
    @Nullable
    public CanaryState getState(CanaryId id) {
        return states.get(id);
    }

    @Override
    public void setState(CanaryId id, CanaryState state) {
        states.put(id, state);
    }
}
