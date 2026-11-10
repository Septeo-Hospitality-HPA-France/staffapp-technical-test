package com.staffapp.housekeeping.task;

import jakarta.validation.constraints.NotNull;

/**
 * @param assigneeId the employee to assign the task to, in the current establishment
 */
public record AssignCleaningTaskRequest(@NotNull Long assigneeId) {
}
