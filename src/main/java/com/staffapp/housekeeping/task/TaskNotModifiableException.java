package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.error.ConflictException;

/**
 * A change the current status of the task does not allow, such as reassigning a done task.
 */
public class TaskNotModifiableException extends ConflictException {

    TaskNotModifiableException(String message) {
        super(message);
    }
}
