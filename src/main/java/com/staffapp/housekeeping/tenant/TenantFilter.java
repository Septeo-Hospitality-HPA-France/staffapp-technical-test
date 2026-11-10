package com.staffapp.housekeeping.tenant;

import com.staffapp.housekeeping.error.ProblemResponses;
import com.staffapp.housekeeping.security.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Reads the establishment selected in the front end from the {@value #ESTABLISHMENT_HEADER} header, checks that the
 * token grants access to it, and exposes it through {@link CurrentEstablishment} for the rest of the request.
 * <p>
 * The header is only trusted once it matches the {@code establishment_id} claim of the signed token: without this
 * check, any user could read another campsite by changing the header. Runs inside the security filter chain, after
 * authentication and authorization, so an anonymous request is a 401 before the header is even read.
 */
public class TenantFilter extends OncePerRequestFilter {

    public static final String ESTABLISHMENT_HEADER = "X-ESTABLISHMENT-ID";

    private static final Logger LOG = LoggerFactory.getLogger(TenantFilter.class);
    private static final RequestMatcher API = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
    private static final Pattern ESTABLISHMENT_ID = Pattern.compile("\\d{1,18}");

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * @param exceptionResolver Spring MVC's resolver, so that errors raised here get the same problem details format
     *                          as the errors raised by the controllers
     */
    public TenantFilter(HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !API.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long establishmentId;
        try {
            establishmentId = authorizedEstablishment(request);
        } catch (ResponseStatusException exception) {
            ProblemResponses.write(exceptionResolver, request, response, exception);
            return;
        }
        CurrentEstablishment.set(establishmentId);
        try {
            chain.doFilter(request, response);
        } finally {
            CurrentEstablishment.clear();
        }
    }

    private Long authorizedEstablishment(HttpServletRequest request) {
        var header = request.getHeader(ESTABLISHMENT_HEADER);
        if (header == null || !ESTABLISHMENT_ID.matcher(header).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing or invalid " + ESTABLISHMENT_HEADER + " header");
        }
        var requested = Long.valueOf(header);

        // Fail closed: /api/** requires a JWT, any other authentication here is a misconfiguration
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to this establishment is denied");
        }
        var granted = establishmentClaim(authentication.getToken());
        if (!requested.equals(granted)) {
            LOG.warn("Establishment access denied: user {} of establishment {} asked for establishment {}",
                    authentication.getName(), granted, requested);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to this establishment is denied");
        }
        return requested;
    }

    /**
     * Returns the establishment of the token, or {@code null} if the claim is missing or not an integer. Strict on
     * purpose: converting a {@code "1"} or a {@code 1.9} would mean guessing which establishment the issuer meant.
     */
    private static Long establishmentClaim(Jwt jwt) {
        Object claim = jwt.getClaim(SecurityConfiguration.ESTABLISHMENT_CLAIM);
        return switch (claim) {
            case Long id -> id;
            case Integer id -> id.longValue();
            case null, default -> null;
        };
    }
}
