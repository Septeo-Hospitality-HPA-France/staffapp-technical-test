package com.staffapp.housekeeping.task;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CleaningTaskRepository
        extends JpaRepository<CleaningTask, Long>, JpaSpecificationExecutor<CleaningTask> {

    /**
     * Use this rather than {@code findById}: a task is only visible from its own establishment. Fetches the room and the
     * assignee in the same query, as the response needs them.
     */
    @EntityGraph(attributePaths = {"room", "assignee"})
    Optional<CleaningTask> findByIdAndEstablishmentId(Long id, Long establishmentId);

    /**
     * Fetches the room and the assignee of every task of the page in the same query: no query per task. The count
     * query of the page does not fetch them.
     */
    @Override
    @EntityGraph(attributePaths = {"room", "assignee"})
    Page<CleaningTask> findAll(Specification<CleaningTask> specification, Pageable pageable);
}
