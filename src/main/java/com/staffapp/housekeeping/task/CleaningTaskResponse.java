package com.staffapp.housekeeping.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A cleaning task, with what the teams need to read without another call: the room number and the assignee's
 * username. Empty fields (no assignee yet, not started) are left out.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CleaningTaskResponse(
        Long id,
        Long roomId,
        String roomNumber,
        LocalDate date,
        CleaningTaskStatus status,
        Long assigneeId,
        String assigneeUsername,
        String comment,
        String createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant doneAt) {

    static CleaningTaskResponse from(CleaningTask task) {
        var assignee = task.getAssignee();
        return new CleaningTaskResponse(
                task.getId(),
                task.getRoom().getId(),
                task.getRoom().getNumber(),
                task.getScheduledOn(),
                task.getStatus(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getUsername(),
                task.getComment(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getDoneAt());
    }
}
