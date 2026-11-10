package com.staffapp.housekeeping.pms;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the RabbitMQ publisher: logs the messages instead of sending them.
 * <p>
 * Only loaded with {@code app.pms.client=logging} (dev profile, tests). Never by default: in production, without the
 * real publisher, the application must refuse to start rather than silently drop every notification.
 */
@Component
@ConditionalOnProperty(name = "app.pms.client", havingValue = "logging")
class LoggingPmsClient implements PmsClient {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPmsClient.class);

    @Override
    public void notifyRoomCleaned(Long roomId, Instant cleanedAt) {
        LOG.info("[FAKE PMS, nothing sent] room {} cleaned at {}", roomId, cleanedAt);
    }
}
