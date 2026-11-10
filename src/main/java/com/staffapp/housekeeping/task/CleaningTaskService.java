package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.error.NotFoundException;
import com.staffapp.housekeeping.room.RoomRepository;
import com.staffapp.housekeeping.staff.Staff;
import com.staffapp.housekeeping.staff.StaffRepository;
import java.time.Clock;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cleaning tasks of an establishment. Every method takes the establishment of the request: a task, a room or an
 * employee of another establishment is reported as not found, never as forbidden, so that ids reveal nothing.
 */
@Service
public class CleaningTaskService {

    private static final String ONE_OPEN_TASK_PER_ROOM = "uk_cleaning_task_one_open_per_room";

    private final CleaningTaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final StaffRepository staffRepository;
    private final Clock clock;

    public CleaningTaskService(CleaningTaskRepository taskRepository, RoomRepository roomRepository,
                               StaffRepository staffRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.staffRepository = staffRepository;
        this.clock = clock;
    }

    /**
     * Plans the cleaning of a room.
     *
     * @throws NotFoundException               if the room or the assignee is not in the establishment
     * @throws RoomAlreadyHasOpenTaskException if the room already has a task to do or in progress
     */
    @Transactional
    public CleaningTaskResponse plan(Long establishmentId, PlanCleaningRequest request, String plannedBy) {
        var room = roomRepository.findByIdAndEstablishmentId(request.roomId(), establishmentId)
                .orElseThrow(() -> new NotFoundException("Room", request.roomId()));
        var assignee = request.assigneeId() == null ? null : findStaff(request.assigneeId(), establishmentId);
        var task = CleaningTask.plan(room, request.date(), assignee, request.comment(), plannedBy, clock.instant());
        // No "exists" check before: it would race with a concurrent planning. The unique index decides, at the flush
        try {
            taskRepository.saveAndFlush(task);
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, ONE_OPEN_TASK_PER_ROOM)) {
                throw new RoomAlreadyHasOpenTaskException(room.getNumber());
            }
            throw exception;
        }
        return CleaningTaskResponse.from(task);
    }

    /**
     * @throws NotFoundException if the task is not in the establishment
     */
    @Transactional(readOnly = true)
    public CleaningTaskResponse get(Long id, Long establishmentId) {
        return CleaningTaskResponse.from(findTask(id, establishmentId));
    }

    private CleaningTask findTask(Long id, Long establishmentId) {
        return taskRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new NotFoundException("Cleaning task", id));
    }

    private Staff findStaff(Long id, Long establishmentId) {
        return staffRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new NotFoundException("Staff", id));
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraint.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
