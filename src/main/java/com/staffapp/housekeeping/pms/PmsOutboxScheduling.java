package com.staffapp.housekeeping.pms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the relay in the background. Disabled in the tests, which run the relay themselves to control its timing.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.pms.relay.enabled", havingValue = "true", matchIfMissing = true)
class PmsOutboxScheduling {

    private final PmsOutboxRelay relay;

    PmsOutboxScheduling(PmsOutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${app.pms.relay.delay:PT5S}")
    void relay() {
        relay.relayPendingNotifications();
    }
}
