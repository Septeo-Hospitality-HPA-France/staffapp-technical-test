package com.staffapp.housekeeping.task;

/**
 * "A task is to do, then in progress, then done. No shortcut."
 */
public enum CleaningTaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    /**
     * Only the next status is reachable: no shortcut from TODO to DONE, no way back. Exhaustive switch on purpose: a new
     * status does not compile until its transitions are decided.
     */
    boolean canMoveTo(CleaningTaskStatus next) {
        return switch (this) {
            case TODO -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE -> false;
        };
    }
}
