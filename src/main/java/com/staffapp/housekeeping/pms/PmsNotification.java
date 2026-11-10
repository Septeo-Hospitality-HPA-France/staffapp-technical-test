package com.staffapp.housekeeping.pms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/**
 * A message for the PMS, kept until the PMS has received it. Refers to the task and the room by id: the outbox does not
 * depend on the domain it reports.
 */
@Entity
class PmsNotification {

    /** Length of the last_error column. */
    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    private Long roomId;

    private Instant cleanedAt;

    private int attempts;

    private Instant sentAt;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    protected PmsNotification() {
    }

    static PmsNotification roomCleaned(Long taskId, Long roomId, Instant cleanedAt) {
        var notification = new PmsNotification();
        notification.taskId = taskId;
        notification.roomId = roomId;
        notification.cleanedAt = cleanedAt;
        return notification;
    }

    void markSent(Instant now) {
        attempts++;
        sentAt = now;
        lastError = null;
    }

    /**
     * Keeps the notification to send: the relay tries again on its next run.
     */
    void markFailed(String error) {
        attempts++;
        lastError = truncate(error);
    }

    /**
     * Never cuts a character in two: a lone half of a surrogate pair would not be valid UTF-8.
     */
    private static String truncate(String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        var end = Character.isHighSurrogate(error.charAt(MAX_ERROR_LENGTH - 1)) ? MAX_ERROR_LENGTH - 1 : MAX_ERROR_LENGTH;
        return error.substring(0, end);
    }

    Long getId() {
        return id;
    }

    Long getRoomId() {
        return roomId;
    }

    Instant getCleanedAt() {
        return cleanedAt;
    }
}
