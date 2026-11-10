package com.staffapp.housekeeping.task;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    /**
     * Use this rather than {@code findById}: a task is only visible from its own establishment. Fetches the room and the
     * assignee in the same query, as the response needs them.
     */
    @EntityGraph(attributePaths = {"room", "assignee"})
    Optional<CleaningTask> findByIdAndEstablishmentId(Long id, Long establishmentId);
}
