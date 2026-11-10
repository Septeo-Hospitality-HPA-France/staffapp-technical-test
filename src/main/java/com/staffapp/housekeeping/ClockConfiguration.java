package com.staffapp.housekeeping;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The only source of time of the application, as a bean so that tests can fix it.
 * <p>
 * Ticks by the microsecond, the precision of PostgreSQL: an instant returned by the API is exactly the one stored, and
 * reads the same once reloaded (the system clock can be finer, 100 ns on Windows).
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
