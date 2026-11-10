package com.staffapp.housekeeping.task;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A page of tasks. A record of our own rather than Spring Data's {@code Page}, whose JSON form is not a stable contract.
 *
 * @param page the number of the page, from 0
 */
public record CleaningTaskPage(
        List<CleaningTaskResponse> content, int page, int size, long totalElements, int totalPages) {

    static CleaningTaskPage from(Page<CleaningTask> tasks) {
        return new CleaningTaskPage(
                tasks.map(CleaningTaskResponse::from).getContent(),
                tasks.getNumber(),
                tasks.getSize(),
                tasks.getTotalElements(),
                tasks.getTotalPages());
    }
}
