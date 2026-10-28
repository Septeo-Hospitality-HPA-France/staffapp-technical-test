package com.staffapp.housekeeping.pms;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the RabbitMQ publisher: logs the messages instead of sending them.
 */
@Component
class LoggingPmsClient implements PmsClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPmsClient.class);

    @Override
    public void notifyRoomCleaned(Long roomId, Instant cleanedAt) {
        LOG.info("PMS notified: room {} cleaned at {}", roomId, cleanedAt);
    }
}
