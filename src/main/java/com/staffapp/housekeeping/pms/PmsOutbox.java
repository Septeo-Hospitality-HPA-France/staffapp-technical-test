package com.staffapp.housekeeping.pms;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the messages for the PMS. They are sent later by {@link PmsOutboxRelay}, never during the request.
 */
@Component
public class PmsOutbox {

    private final PmsNotificationRepository repository;

    PmsOutbox(PmsNotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Records that a room was cleaned. MANDATORY on purpose: the notification must be written in the transaction that
     * marks the room clean, so that both are committed, or rolled back, together.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void roomCleaned(Long taskId, Long roomId, Instant cleanedAt) {
        repository.save(PmsNotification.roomCleaned(taskId, roomId, cleanedAt));
    }
}
