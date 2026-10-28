package com.staffapp.housekeeping.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the establishment selected in the front end from the {@value #ESTABLISHMENT_HEADER} header and exposes it
 * through {@link CurrentEstablishment} for the rest of the request.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String ESTABLISHMENT_HEADER = "X-ESTABLISHMENT-ID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader(ESTABLISHMENT_HEADER);
        if (header == null || !header.matches("\\d{1,18}")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing or invalid " + ESTABLISHMENT_HEADER + " header");
            return;
        }
        CurrentEstablishment.set(Long.valueOf(header));
        try {
            chain.doFilter(request, response);
        } finally {
            CurrentEstablishment.clear();
        }
    }
}
