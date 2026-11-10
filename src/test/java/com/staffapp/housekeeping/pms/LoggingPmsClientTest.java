package com.staffapp.housekeeping.pms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LoggingPmsClientTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LoggingPmsClient.class);

    @Test
    @DisplayName("Never stands in for the PMS by default: without a real client, the application must not start")
    void isNotLoadedByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PmsClient.class));
    }

    @Test
    @DisplayName("Stands in for the PMS only when asked to, locally and in tests")
    void isLoadedWhenAskedTo() {
        runner.withPropertyValues("app.pms.client=logging")
                .run(context -> assertThat(context).hasSingleBean(PmsClient.class));
    }
}
