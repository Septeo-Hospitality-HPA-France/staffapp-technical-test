package com.staffapp.housekeeping.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.staffapp.housekeeping.establishment.Establishment;
import com.staffapp.housekeeping.room.Room;
import com.staffapp.housekeeping.staff.Staff;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CleaningTaskTest {

    private static final LocalDate DAY = LocalDate.parse("2026-07-14");
    private static final Instant PLANNED_AT = Instant.parse("2026-07-14T07:00:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-07-14T09:00:00Z");
    private static final Instant DONE_AT = Instant.parse("2026-07-14T10:00:00Z");

    private final Establishment lesPins = establishment(1L);
    private final Room room = room(lesPins);
    private final CleaningTask task = CleaningTask.plan(room, DAY, null, "Check the terrace", "marc", PLANNED_AT);

    @Test
    @DisplayName("A planned task is to do, not started, in the establishment of its room")
    void aPlannedTaskIsToDo() {
        assertThat(task.getStatus()).isEqualTo(CleaningTaskStatus.TODO);
        assertThat(task.getEstablishment()).isSameAs(lesPins);
        assertThat(task.getStartedAt()).isNull();
        assertThat(task.getDoneAt()).isNull();
    }

    @Test
    @DisplayName("Refuses an assignee of another establishment than the room")
    void refusesAnAssigneeOfAnotherEstablishment() {
        var claire = staff(establishment(2L));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CleaningTask.plan(room, DAY, claire, null, "marc", PLANNED_AT));
    }

    @Test
    @DisplayName("Refuses a comment longer than the database column")
    void refusesACommentTooLong() {
        var comment = "x".repeat(CleaningTask.MAX_COMMENT_LENGTH + 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CleaningTask.plan(room, DAY, null, comment, "marc", PLANNED_AT));
    }

    @Test
    @DisplayName("Starting a task to do puts it in progress")
    void startingATaskPutsItInProgress() {
        task.start(STARTED_AT);

        assertThat(task.getStatus()).isEqualTo(CleaningTaskStatus.IN_PROGRESS);
        assertThat(task.getStartedAt()).isEqualTo(STARTED_AT);
    }

    @Test
    @DisplayName("Completing a task in progress makes it done and hands the room over as cleaned")
    void completingATaskInProgressMakesItDone() {
        task.start(STARTED_AT);
        task.complete(DONE_AT);

        assertThat(task.getStatus()).isEqualTo(CleaningTaskStatus.DONE);
        assertThat(task.getDoneAt()).isEqualTo(DONE_AT);
        verify(room).markCleaned(DONE_AT);
    }

    @Test
    @DisplayName("No shortcut: a task never started cannot be completed, and its room stays as it was")
    void refusesToCompleteATaskNeverStarted() {
        assertThatThrownBy(() -> task.complete(DONE_AT))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot move a task from TODO to DONE");

        assertThat(task.getStatus()).isEqualTo(CleaningTaskStatus.TODO);
        verify(room, never()).markCleaned(any());
    }

    @Test
    @DisplayName("No way back: a task cannot be started twice, nor completed twice")
    void refusesToRepeatATransition() {
        task.start(STARTED_AT);
        assertThatThrownBy(() -> task.start(DONE_AT)).isInstanceOf(InvalidTransitionException.class);

        task.complete(DONE_AT);
        assertThatThrownBy(() -> task.complete(Instant.now())).isInstanceOf(InvalidTransitionException.class);
        assertThat(task.getDoneAt()).isEqualTo(DONE_AT);
    }

    @Test
    @DisplayName("Refuses a status change without its time, and leaves the task unchanged")
    void refusesAStatusChangeWithoutItsTime() {
        assertThatNullPointerException().isThrownBy(() -> task.start(null));

        assertThat(task.getStatus()).isEqualTo(CleaningTaskStatus.TODO);
    }

    private static Establishment establishment(Long id) {
        var establishment = mock(Establishment.class);
        when(establishment.getId()).thenReturn(id);
        return establishment;
    }

    private static Room room(Establishment establishment) {
        var room = mock(Room.class);
        when(room.getEstablishment()).thenReturn(establishment);
        return room;
    }

    private static Staff staff(Establishment establishment) {
        var staff = mock(Staff.class);
        when(staff.getEstablishment()).thenReturn(establishment);
        return staff;
    }
}
