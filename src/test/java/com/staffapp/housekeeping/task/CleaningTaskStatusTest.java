package com.staffapp.housekeeping.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CleaningTaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "TODO,        TODO,        false",
            "TODO,        IN_PROGRESS, true",
            "TODO,        DONE,        false",
            "IN_PROGRESS, TODO,        false",
            "IN_PROGRESS, IN_PROGRESS, false",
            "IN_PROGRESS, DONE,        true",
            "DONE,        TODO,        false",
            "DONE,        IN_PROGRESS, false",
            "DONE,        DONE,        false"
    })
    @DisplayName("A task is to do, then in progress, then done: no shortcut, no way back")
    void allowsOnlyTheForwardTransitions(CleaningTaskStatus from, CleaningTaskStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
