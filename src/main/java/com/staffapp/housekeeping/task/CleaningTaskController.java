package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.tenant.CurrentEstablishment;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The roles are checked in {@link com.staffapp.housekeeping.security.SecurityConfiguration}, before the request body is
 * read: a {@code @PreAuthorize} here would only run after the validation, and answer a 400 to an employee.
 */
@RestController
@RequestMapping(CleaningTaskController.PATH)
class CleaningTaskController {

    static final String PATH = "/api/cleaning-tasks";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final CleaningTaskService taskService;

    CleaningTaskController(CleaningTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * "Only managers plan the cleaning."
     */
    @PostMapping
    ResponseEntity<CleaningTaskResponse> plan(@Valid @RequestBody PlanCleaningRequest request,
                                              Authentication authentication) {
        var task = taskService.plan(CurrentEstablishment.getId(), request, authentication.getName());
        // From the context path, not the current request: its query string or trailing slash must not leak in
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(PATH + "/{id}")
                .buildAndExpand(task.id())
                .toUri();
        return ResponseEntity.created(location).body(task);
    }

    /**
     * The tasks of a day, of a status, or both: at least one of them, as the teams look at their day or at what is
     * left to do, never at the whole history.
     */
    @GetMapping
    CleaningTaskPage list(@RequestParam(required = false) LocalDate date,
                          @RequestParam(required = false) CleaningTaskStatus status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (date == null && status == null) {
            throw badRequest("Filter the tasks by date, by status, or both");
        }
        if (page < 0) {
            throw badRequest("page must be 0 or more");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return taskService.list(CurrentEstablishment.getId(), date, status, page, size);
    }

    @GetMapping("/{id}")
    CleaningTaskResponse get(@PathVariable Long id) {
        return taskService.get(id, CurrentEstablishment.getId());
    }

    /**
     * Assigns a planned task later, or hands it over: managers only.
     */
    @PatchMapping("/{id}")
    CleaningTaskResponse assign(@PathVariable Long id, @Valid @RequestBody AssignCleaningTaskRequest request) {
        return taskService.assign(id, CurrentEstablishment.getId(), request.assigneeId());
    }

    /**
     * Action endpoints rather than a PATCH of the status: the client states what it does, and cannot ask for a status
     * the workflow does not allow, such as DONE directly.
     */
    @PostMapping("/{id}/start")
    CleaningTaskResponse start(@PathVariable Long id) {
        return taskService.start(id, CurrentEstablishment.getId());
    }

    @PostMapping("/{id}/complete")
    CleaningTaskResponse complete(@PathVariable Long id) {
        return taskService.complete(id, CurrentEstablishment.getId());
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
