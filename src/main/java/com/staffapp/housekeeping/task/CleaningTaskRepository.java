package com.staffapp.housekeeping.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {
}
