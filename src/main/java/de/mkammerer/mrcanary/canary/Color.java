package de.mkammerer.mrcanary.canary;

public enum Color {
    /**
     * Routing all traffic to blue backend.
     */
    BLUE,
    /**
     * Routing all traffic to green backend.
     */
    GREEN,
    /**
     * Backend is shifting.
     */
    SHIFTING
}
