package com.staffapp.housekeeping.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * @param roomId     the accommodation to clean, in the current establishment
 * @param date       the day of the cleaning
 * @param assigneeId the employee in charge, in the current establishment, or {@code null} to assign it later
 * @param comment    free text for the team ("check the terrace", "linen for 6"), optional
 */
public record PlanCleaningRequest(
        @NotNull Long roomId,
        @NotNull LocalDate date,
        Long assigneeId,
        @Size(max = CleaningTask.MAX_COMMENT_LENGTH) String comment) {
}
