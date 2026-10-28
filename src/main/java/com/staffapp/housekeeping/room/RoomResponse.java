package com.staffapp.housekeeping.room;

import java.time.Instant;

public record RoomResponse(Long id, String number, RoomType type, Instant lastCleanedAt) {

    static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getNumber(), room.getType(), room.getLastCleanedAt());
    }
}
