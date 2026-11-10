package com.staffapp.housekeeping.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Writes an error raised outside the controllers (in a filter or a security handler) in the problem details format of
 * the API, through Spring MVC's exception resolver.
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    /**
     * If no resolver writes the error, still sends its status: a refusal must never end as an empty 200.
     */
    public static void write(HandlerExceptionResolver resolver, HttpServletRequest request,
                             HttpServletResponse response, ResponseStatusException error) throws IOException {
        if (resolver.resolveException(request, response, null, error) == null) {
            response.sendError(error.getStatusCode().value(), error.getReason());
        }
    }
}
