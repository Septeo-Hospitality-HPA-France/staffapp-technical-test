package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.error.ConflictException;

/**
 * "Never two cleaning tasks open on the same accommodation: the teams would get in each other's way."
 */
public class RoomAlreadyHasOpenTaskException extends ConflictException {

    RoomAlreadyHasOpenTaskException(String roomNumber) {
        super("Room " + roomNumber + " already has an open cleaning task");
    }
}
