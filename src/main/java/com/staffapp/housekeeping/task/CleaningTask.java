package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.establishment.Establishment;
import com.staffapp.housekeeping.room.Room;
import com.staffapp.housekeeping.staff.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The cleaning of an accommodation on a given day. Its status only changes through {@link #start} and
 * {@link #complete}, which enforce "to do, then in progress, then done".
 */
@Entity
public class CleaningTask {

    /** Length of the comment column. */
    public static final int MAX_COMMENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Establishment establishment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    private Staff assignee;

    private LocalDate scheduledOn;

    @Enumerated(EnumType.STRING)
    private CleaningTaskStatus status;

    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    private String createdBy;

    private Instant createdAt;

    private Instant startedAt;

    private Instant doneAt;

    // Two people changing the same task at the same time: the second one fails instead of overwriting the first
    @Version
    private long version;

    protected CleaningTask() {
    }

    /**
     * Plans the cleaning of a room. The task belongs to the establishment of the room.
     * <p>
     * The checks below are invariants, not input validation: the service validates the request first (400, 404).
     *
     * @param assignee  the employee in charge, or {@code null} to assign the task later
     * @param comment   free text for the team, or {@code null}
     * @param createdBy the username of the manager who plans it
     * @throws IllegalArgumentException if the assignee belongs to another establishment than the room, or the comment
     *                                  is longer than {@value #MAX_COMMENT_LENGTH} characters
     */
    public static CleaningTask plan(
            Room room, LocalDate scheduledOn, Staff assignee, String comment, String createdBy, Instant now) {
        var establishment = Objects.requireNonNull(room.getEstablishment(), "room.establishment");
        if (assignee != null) {
            requireSameEstablishment(establishment, assignee);
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("The comment must not exceed " + MAX_COMMENT_LENGTH + " characters");
        }
        var task = new CleaningTask();
        task.establishment = establishment;
        task.room = room;
        task.scheduledOn = Objects.requireNonNull(scheduledOn, "scheduledOn");
        task.assignee = assignee;
        task.comment = comment;
        task.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        task.createdAt = Objects.requireNonNull(now, "now");
        task.status = CleaningTaskStatus.TODO;
        return task;
    }

    /**
     * @throws InvalidTransitionException if the task is not to do
     */
    public void start(Instant now) {
        Objects.requireNonNull(now, "now");
        moveTo(CleaningTaskStatus.IN_PROGRESS);
        startedAt = now;
    }

    /**
     * Completes the task and hands the room over as cleaned, in the same transaction.
     *
     * @throws InvalidTransitionException if the task is not in progress
     */
    public void complete(Instant now) {
        Objects.requireNonNull(now, "now");
        moveTo(CleaningTaskStatus.DONE);
        doneAt = now;
        room.markCleaned(now);
    }

    /**
     * Assigns the task, or hands it over to another employee, until it is done.
     *
     * @throws TaskNotModifiableException if the task is done
     * @throws IllegalArgumentException   if the employee belongs to another establishment
     */
    public void assignTo(Staff staff) {
        if (status == CleaningTaskStatus.DONE) {
            throw new TaskNotModifiableException("A done task cannot be reassigned");
        }
        requireSameEstablishment(establishment, staff);
        assignee = staff;
    }

    private static void requireSameEstablishment(Establishment establishment, Staff staff) {
        if (!establishment.getId().equals(staff.getEstablishment().getId())) {
            throw new IllegalArgumentException("The assignee must belong to the establishment of the room");
        }
    }

    private void moveTo(CleaningTaskStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        status = next;
    }

    public Long getId() {
        return id;
    }

    public Establishment getEstablishment() {
        return establishment;
    }

    public Room getRoom() {
        return room;
    }

    public Staff getAssignee() {
        return assignee;
    }

    public LocalDate getScheduledOn() {
        return scheduledOn;
    }

    public CleaningTaskStatus getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getDoneAt() {
        return doneAt;
    }
}
