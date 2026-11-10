package com.staffapp.housekeeping.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("A conflict with the state of a resource is a 409 with its reason")
    void aConflictIsA409() {
        var problem = handler.handleConflict(new ConflictException("Cannot move a task from TODO to DONE") {
        });

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Cannot move a task from TODO to DONE");
    }
}
