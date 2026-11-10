package com.staffapp.housekeeping.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class ProblemResponsesTest {

    @Test
    @DisplayName("Still sends the status when no resolver writes the error: a refusal never ends as an empty 200")
    void sendsTheStatusWhenNoResolverWritesTheError() throws Exception {
        var response = new MockHttpServletResponse();

        ProblemResponses.write((request, ignored, handler, exception) -> null, new MockHttpServletRequest(), response,
                new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to do this"));

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
