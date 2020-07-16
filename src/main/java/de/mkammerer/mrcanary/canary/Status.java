package de.mkammerer.mrcanary.canary;

import lombok.Getter;

/**
 * Good case: INIT_BLUE -> SHIFT_TO_GREEN -> SUCCESS_GREEN
 * <p>
 * Bad case: INIT_BLUE -> SHIFT_TO_GREEN -> FAILED_BLUE
 */
public enum Status {
    INIT_BLUE(Color.BLUE),
    INIT_GREEN(Color.GREEN),
    SHIFT_TO_BLUE(Color.SHIFTING),
    SHIFT_TO_GREEN(Color.SHIFTING),
    SUCCESS_BLUE(Color.BLUE),
    SUCCESS_GREEN(Color.GREEN),
    FAILED_BLUE(Color.BLUE),
    FAILED_GREEN(Color.GREEN);

    @Getter
    private final Color backendColor;

    Status(Color backendColor) {
        this.backendColor = backendColor;
    }
}
