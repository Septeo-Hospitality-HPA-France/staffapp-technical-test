package com.staffapp.housekeeping.pms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.staffapp.housekeeping.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * "The PMS is what the reception trusts. If it is never told, the guest waits at the desk." The relay keeps a
 * notification until the PMS has received it.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PmsOutboxRelayTest {

    private static final long ROOM_A01 = 1L;
    private static final long ROOM_A02 = 2L;
    private static final Instant CLEANED_AT = Instant.parse("2026-07-14T10:00:00Z");

    @MockitoBean
    private PmsClient pmsClient;

    @Autowired
    private PmsOutboxRelay relay;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM pms_notification");
        jdbcTemplate.update("DELETE FROM cleaning_task");
    }

    @Test
    @DisplayName("Sends a pending notification to the PMS, once")
    void sendsAPendingNotificationOnce() {
        var id = notificationOfACompletedTask(ROOM_A01);

        relay.relayPendingNotifications();
        relay.relayPendingNotifications();

        verify(pmsClient, times(1)).notifyRoomCleaned(ROOM_A01, CLEANED_AT);
        assertThat(notification(id).get("sent_at")).isNotNull();
    }

    @Test
    @DisplayName("Keeps a notification the PMS did not receive, and sends it on a later run")
    void keepsANotificationUntilThePmsReceivesIt() {
        var id = notificationOfACompletedTask(ROOM_A01);
        doThrow(new IllegalStateException("RabbitMQ unreachable"))
                .doNothing()
                .when(pmsClient).notifyRoomCleaned(any(), any());

        relay.relayPendingNotifications();

        var failed = notification(id);
        assertThat(failed.get("sent_at")).isNull();
        assertThat(failed.get("attempts")).isEqualTo(1);
        assertThat(failed.get("last_error")).asString().contains("RabbitMQ unreachable");

        relay.relayPendingNotifications();

        verify(pmsClient, times(2)).notifyRoomCleaned(ROOM_A01, CLEANED_AT);
        assertThat(notification(id).get("sent_at")).isNotNull();
    }

    @Test
    @DisplayName("A notification the PMS rejects does not block the others")
    void aFailureDoesNotBlockTheOthers() {
        var failing = notificationOfACompletedTask(ROOM_A01);
        var other = notificationOfACompletedTask(ROOM_A02);
        doThrow(new IllegalStateException("Rejected")).when(pmsClient).notifyRoomCleaned(eq(ROOM_A01), any());

        relay.relayPendingNotifications();

        assertThat(notification(failing).get("sent_at")).isNull();
        assertThat(notification(other).get("sent_at")).isNotNull();
    }

    @Test
    @DisplayName("Calls the PMS outside any transaction: a slow PMS holds no database connection")
    void callsThePmsOutsideAnyTransaction() {
        notificationOfACompletedTask(ROOM_A01);
        var inTransaction = new AtomicBoolean(true);
        doAnswer(invocation -> {
            inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(pmsClient).notifyRoomCleaned(any(), any());

        relay.relayPendingNotifications();

        assertThat(inTransaction).isFalse();
    }

    private Long notificationOfACompletedTask(long roomId) {
        var taskId = jdbcTemplate.queryForObject("""
                INSERT INTO cleaning_task (establishment_id, room_id, scheduled_on, status, created_by, created_at,
                                           started_at, done_at)
                VALUES (1, ?, CURRENT_DATE, 'DONE', 'marc', now(), now(), ?)
                RETURNING id""", Long.class, roomId, Timestamp.from(CLEANED_AT));
        return jdbcTemplate.queryForObject("""
                INSERT INTO pms_notification (task_id, room_id, cleaned_at)
                VALUES (?, ?, ?)
                RETURNING id""", Long.class, taskId, roomId, Timestamp.from(CLEANED_AT));
    }

    private Map<String, Object> notification(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM pms_notification WHERE id = ?", id);
    }
}
