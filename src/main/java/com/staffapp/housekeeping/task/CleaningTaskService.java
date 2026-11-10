package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.error.NotFoundException;
import com.staffapp.housekeeping.pms.PmsOutbox;
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
    private final PmsOutbox pmsOutbox;
    private final Clock clock;

    public CleaningTaskService(CleaningTaskRepository taskRepository, RoomRepository roomRepository,
                               StaffRepository staffRepository, PmsOutbox pmsOutbox, Clock clock) {
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.staffRepository = staffRepository;
        this.pmsOutbox = pmsOutbox;
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

    /**
     * Marks a task as started, whoever it is assigned to: any employee of the campsite can take it over.
     *
     * @throws NotFoundException          if the task is not in the establishment
     * @throws InvalidTransitionException if the task is not to do
     */
    @Transactional
    public CleaningTaskResponse start(Long id, Long establishmentId) {
        var task = findTask(id, establishmentId);
        task.start(clock.instant());
        return CleaningTaskResponse.from(task);
    }

    /**
     * Marks a task as done and hands the room over. In one transaction: the task is done, the room is marked cleaned
     * and the PMS notification is recorded, or none of them is. The notification is sent after the commit, by the
     * outbox relay: the PMS is never told a room is clean before it really is.
     *
     * @throws NotFoundException          if the task is not in the establishment
     * @throws InvalidTransitionException if the task is not in progress
     */
    @Transactional
    public CleaningTaskResponse complete(Long id, Long establishmentId) {
        var task = findTask(id, establishmentId);
        var now = clock.instant();
        task.complete(now);
        pmsOutbox.roomCleaned(task.getId(), task.getRoom().getId(), now);
        return CleaningTaskResponse.from(task);
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
