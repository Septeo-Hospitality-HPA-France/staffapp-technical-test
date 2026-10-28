package com.staffapp.housekeeping.pms;

import java.time.Instant;

/**
 * Client of the property management system (PMS), which handles bookings and check-ins. In production, messages are
 * published to RabbitMQ.
 */
public interface PmsClient {

    /**
     * Tells the PMS that a room is clean and can be handed over to arriving guests.
     */
    void notifyRoomCleaned(Long roomId, Instant cleanedAt);
}
