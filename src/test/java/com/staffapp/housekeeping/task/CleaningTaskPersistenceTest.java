package com.staffapp.housekeeping.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.staffapp.housekeeping.TestcontainersConfiguration;
import com.staffapp.housekeeping.room.RoomRepository;
import com.staffapp.housekeeping.staff.StaffRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The database itself enforces the rules, whatever the code above it does: the schema tests below insert rows with
 * plain SQL, without going through the entity.
 */
@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class CleaningTaskPersistenceTest {

    private static final long LES_PINS = 1L;
    private static final long ROOM_A01 = 1L;
    private static final long ROOM_B05 = 15L;
    private static final long CLAIRE = 4L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CleaningTaskRepository taskRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Never two open tasks on the same accommodation")
    void refusesTwoOpenTasksOnTheSameRoom() {
        insertTask(LES_PINS, ROOM_A01, "TODO");

        assertThatThrownBy(() -> insertTask(LES_PINS, ROOM_A01, "TODO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_cleaning_task_one_open_per_room");
    }

    @Test
    @DisplayName("A new task can be planned once the previous one is done")
    void acceptsANewTaskOnceThePreviousOneIsDone() {
        jdbcTemplate.update("""
                INSERT INTO cleaning_task (establishment_id, room_id, scheduled_on, status, created_by, created_at,
                                           started_at, done_at)
                VALUES (?, ?, CURRENT_DATE, 'DONE', 'marc', now(), now(), now())""", LES_PINS, ROOM_A01);

        assertThatNoException().isThrownBy(() -> insertTask(LES_PINS, ROOM_A01, "TODO"));
    }

    @Test
    @DisplayName("A task cannot point to an accommodation of another establishment")
    void refusesARoomOfAnotherEstablishment() {
        assertThatThrownBy(() -> insertTask(LES_PINS, ROOM_B05, "TODO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cleaning_task_room");
    }

    @Test
    @DisplayName("A task cannot be assigned to an employee of another establishment")
    void refusesAnAssigneeOfAnotherEstablishment() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO cleaning_task (establishment_id, room_id, assignee_id, scheduled_on, status, created_by,
                                           created_at)
                VALUES (?, ?, ?, CURRENT_DATE, 'TODO', 'marc', now())""", LES_PINS, ROOM_A01, CLAIRE))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cleaning_task_assignee");
    }

    @Test
    @DisplayName("A task marked done must carry its start and end times")
    void refusesADoneTaskWithoutItsTimes() {
        assertThatThrownBy(() -> insertTask(LES_PINS, ROOM_A01, "DONE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cleaning_task_status");
    }

    @Test
    @DisplayName("Saves and reloads a planned task")
    void savesAndReloadsAPlannedTask() {
        var room = roomRepository.findById(ROOM_A01).orElseThrow();
        var alice = staffRepository.findByUsername("alice").orElseThrow();
        var plannedAt = Instant.parse("2026-07-14T07:00:00Z");
        var task = taskRepository.saveAndFlush(CleaningTask.plan(
                room, LocalDate.parse("2026-07-14"), alice, "Linen for 6", "marc", plannedAt));
        entityManager.clear();

        var reloaded = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(CleaningTaskStatus.TODO);
        assertThat(reloaded.getEstablishment().getId()).isEqualTo(LES_PINS);
        assertThat(reloaded.getRoom().getId()).isEqualTo(ROOM_A01);
        assertThat(reloaded.getAssignee().getId()).isEqualTo(alice.getId());
        assertThat(reloaded.getScheduledOn()).isEqualTo(LocalDate.parse("2026-07-14"));
        assertThat(reloaded.getComment()).isEqualTo("Linen for 6");
        assertThat(reloaded.getCreatedBy()).isEqualTo("marc");
        assertThat(reloaded.getCreatedAt()).isEqualTo(plannedAt);
    }

    @Test
    @DisplayName("Completing a task saves the room as cleaned at the completion time")
    void completingATaskSavesTheRoomAsCleaned() {
        var room = roomRepository.findById(ROOM_A01).orElseThrow();
        var task = CleaningTask.plan(room, LocalDate.parse("2026-07-14"), null, null, "marc", Instant.now());
        var doneAt = Instant.parse("2026-07-14T10:00:00Z");
        task.start(Instant.parse("2026-07-14T09:00:00Z"));
        task.complete(doneAt);
        taskRepository.saveAndFlush(task);
        entityManager.clear();

        assertThat(roomRepository.findById(ROOM_A01).orElseThrow().getLastCleanedAt()).isEqualTo(doneAt);
    }

    private void insertTask(long establishmentId, long roomId, String status) {
        jdbcTemplate.update("""
                INSERT INTO cleaning_task (establishment_id, room_id, scheduled_on, status, created_by, created_at)
                VALUES (?, ?, CURRENT_DATE, ?, 'marc', now())""", establishmentId, roomId, status);
    }
}
