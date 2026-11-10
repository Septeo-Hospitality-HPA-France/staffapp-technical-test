package com.staffapp.housekeeping.error;

/**
 * A resource that does not exist in the current establishment. Also used for resources of another establishment, so
 * that an id never reveals whether it exists elsewhere.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " " + id + " not found");
    }
}
