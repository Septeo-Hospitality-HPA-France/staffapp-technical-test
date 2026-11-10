package com.staffapp.housekeeping.task;

import com.staffapp.housekeeping.tenant.CurrentEstablishment;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The roles are checked in {@link com.staffapp.housekeeping.security.SecurityConfiguration}, before the request body is
 * read: a {@code @PreAuthorize} here would only run after the validation, and answer a 400 to an employee.
 */
@RestController
@RequestMapping(CleaningTaskController.PATH)
class CleaningTaskController {

    static final String PATH = "/api/cleaning-tasks";

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

    @GetMapping("/{id}")
    CleaningTaskResponse get(@PathVariable Long id) {
        return taskService.get(id, CurrentEstablishment.getId());
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
}
