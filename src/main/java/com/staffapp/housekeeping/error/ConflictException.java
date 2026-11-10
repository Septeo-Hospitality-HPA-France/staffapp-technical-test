package com.staffapp.housekeeping.error;

/**
 * A request that is valid but conflicts with the current state of a resource.
 */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
