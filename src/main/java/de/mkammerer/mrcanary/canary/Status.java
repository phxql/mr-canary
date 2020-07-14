package de.mkammerer.mrcanary.canary;

/**
 * Good case: INIT_BLUE -> SHIFT_TO_GREEN -> SUCCESS_GREEN
 * <p>
 * Bad case: INIT_BLUE -> SHIFT_TO_GREEN -> FAILED_BLUE
 */
public enum Status {
    INIT_BLUE,
    INIT_GREEN,
    SHIFT_TO_BLUE,
    SHIFT_TO_GREEN,
    SUCCESS_BLUE,
    SUCCESS_GREEN,
    FAILED_BLUE,
    FAILED_GREEN
}
