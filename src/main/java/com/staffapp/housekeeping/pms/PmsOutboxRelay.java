package com.staffapp.housekeeping.pms;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends the pending notifications to the PMS, oldest first. The PMS is called outside any transaction, so that a slow
 * PMS holds no database connection; each result is then saved on its own. A notification the PMS did not receive stays
 * pending and is sent again on the next run.
 * <p>
 * Delivery is at least once: if the application stops between the call and the save, the notification is sent again.
 * "Room cleaned at T" is idempotent for the PMS, so a duplicate is harmless, unlike a lost message.
 */
@Component
class PmsOutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(PmsOutboxRelay.class);

    private final PmsNotificationRepository repository;
    private final PmsClient pmsClient;
    private final Clock clock;

    PmsOutboxRelay(PmsNotificationRepository repository, PmsClient pmsClient, Clock clock) {
        this.repository = repository;
        this.pmsClient = pmsClient;
        this.clock = clock;
    }

    void relayPendingNotifications() {
        for (var notification : repository.findTop100BySentAtIsNullOrderByIdAsc()) {
            try {
                pmsClient.notifyRoomCleaned(notification.getRoomId(), notification.getCleanedAt());
                notification.markSent(clock.instant());
            } catch (Exception exception) {
                // Exception, not only RuntimeException: a client may throw a checked exception it does not declare
                notification.markFailed(exception.getMessage());
                LOG.warn("PMS notification {} failed, will retry: {}", notification.getId(), exception.getMessage());
            }
            repository.save(notification);
        }
    }
}
