package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.error.ConflictException;

/**
 * A status change that skips a step or goes back, such as completing a task that was never started.
 */
public class InvalidTransitionException extends ConflictException {

    InvalidTransitionException(CleaningTaskStatus from, CleaningTaskStatus to) {
        super("Cannot move a task from " + from + " to " + to);
    }
}
