package com.staffapp.housekeeping.task;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * The filters of the task list. Only the filters given are added to the query, rather than
 * {@code (:date IS NULL OR scheduled_on = :date)}, which would keep PostgreSQL from using the indexes on
 * (establishment_id, scheduled_on, status) and (establishment_id, status).
 */
final class CleaningTaskSpecifications {

    private CleaningTaskSpecifications() {
    }

    static Specification<CleaningTask> search(Long establishmentId, LocalDate day, CleaningTaskStatus status) {
        Specification<CleaningTask> specification = inEstablishment(establishmentId);
        if (day != null) {
            specification = specification.and(scheduledOn(day));
        }
        if (status != null) {
            specification = specification.and(withStatus(status));
        }
        return specification;
    }

    private static Specification<CleaningTask> inEstablishment(Long establishmentId) {
        return (task, query, builder) -> builder.equal(task.get("establishment").get("id"), establishmentId);
    }

    private static Specification<CleaningTask> scheduledOn(LocalDate day) {
        return (task, query, builder) -> builder.equal(task.get("scheduledOn"), day);
    }

    private static Specification<CleaningTask> withStatus(CleaningTaskStatus status) {
        return (task, query, builder) -> builder.equal(task.get("status"), status);
    }
}
