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
    FAILED_GREEN;

    public boolean isRunning() {
        return this == SHIFT_TO_GREEN || this == SHIFT_TO_BLUE;
    }

    public Color getBackendColor() {
        switch (this) {
            case INIT_BLUE:
            case SUCCESS_BLUE:
            case FAILED_BLUE:
                return Color.BLUE;
            case INIT_GREEN:
            case SUCCESS_GREEN:
            case FAILED_GREEN:
                return Color.GREEN;
            case SHIFT_TO_BLUE:
            case SHIFT_TO_GREEN:
                return Color.SHIFTING;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Status getStartStatus() {
        switch (this) {
            case SHIFT_TO_BLUE:
            case SHIFT_TO_GREEN:
                throw new IllegalStateException(String.format("State %s has no start status", this));
            case INIT_BLUE:
            case FAILED_BLUE:
            case SUCCESS_BLUE:
                // Current backend is blue -> shift to green
                return Status.SHIFT_TO_GREEN;
            case INIT_GREEN:
            case FAILED_GREEN:
            case SUCCESS_GREEN:
                // Current backend is green -> shift to blue
                return Status.SHIFT_TO_BLUE;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Status getAbortStatus() {
        return getFailureStatus();
    }

    public Status getSuccessStatus() {
        switch (this) {
            case SUCCESS_BLUE:
            case SUCCESS_GREEN:
            case FAILED_BLUE:
            case FAILED_GREEN:
            case INIT_BLUE:
            case INIT_GREEN:
                throw new IllegalStateException(String.format("State %s has no success status", this));
            case SHIFT_TO_BLUE:
                // Success while shifting to blue results in blue backend used
                return SUCCESS_BLUE;
            case SHIFT_TO_GREEN:
                // Success while shifting to green results in green backend used
                return SUCCESS_GREEN;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Status getFailureStatus() {
        switch (this) {
            case SUCCESS_BLUE:
            case SUCCESS_GREEN:
            case FAILED_BLUE:
            case FAILED_GREEN:
            case INIT_BLUE:
            case INIT_GREEN:
                throw new IllegalStateException(String.format("State %s has no failure status", this));
            case SHIFT_TO_BLUE:
                // Failure while shifting to blue results in green backend used
                return FAILED_GREEN;
            case SHIFT_TO_GREEN:
                // Failure while shifting to green results in blue backend used
                return FAILED_BLUE;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Color getPrimaryColor() {
        switch (this) {
            case SUCCESS_BLUE:
            case SUCCESS_GREEN:
            case FAILED_BLUE:
            case FAILED_GREEN:
            case INIT_BLUE:
            case INIT_GREEN:
                throw new IllegalStateException(String.format("State %s has no primary color", this));
            case SHIFT_TO_BLUE:
                // Shift from primary green to canary blue
                return Color.GREEN;
            case SHIFT_TO_GREEN:
                // Shift from primary blue to canary green
                return Color.BLUE;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Color getCanaryColor() {
        switch (this) {
            case SUCCESS_BLUE:
            case SUCCESS_GREEN:
            case FAILED_BLUE:
            case FAILED_GREEN:
            case INIT_BLUE:
            case INIT_GREEN:
                throw new IllegalStateException(String.format("State %s has no canary color", this));
            case SHIFT_TO_BLUE:
                // Shift from primary green to canary blue
                return Color.BLUE;
            case SHIFT_TO_GREEN:
                // Shift from primary blue to canary green
                return Color.GREEN;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
