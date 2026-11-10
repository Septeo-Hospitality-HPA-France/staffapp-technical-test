package com.staffapp.housekeeping.security;

import com.staffapp.housekeeping.error.ProblemResponses;
import com.staffapp.housekeeping.tenant.TenantFilter;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless JWT authentication. A token carries the username ({@code sub}), the establishment
 * ({@code establishment_id}) and the role ({@code role}), exposed as the {@code ROLE_STAFF} or
 * {@code ROLE_MANAGER} authority.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    public static final String ESTABLISHMENT_CLAIM = "establishment_id";
    public static final String ROLE_CLAIM = "role";

    /**
     * The roles are checked here, by URL, rather than with {@code @PreAuthorize}: the security chain runs before the
     * request body is read, so a forbidden action is always a 403, whatever its body.
     * <p>
     * The {@link TenantFilter} is created here rather than declared as a bean: Spring Boot would otherwise also register
     * it as a servlet filter, outside the security chain, where its position relative to authentication is implicit.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // "Only managers plan the cleaning"
                        .requestMatchers(HttpMethod.POST, "/api/cleaning-tasks").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Same problem details format as the errors of the controllers. Only authenticated requests get here:
                // an anonymous one goes to the authentication entry point, a 401
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, denied) ->
                        ProblemResponses.write(exceptionResolver, request, response,
                                new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to do this"))))
                .addFilterAfter(new TenantFilter(exceptionResolver), AuthorizationFilter.class);
        return http.build();
    }

    /**
     * The signing key has no default value: without {@code app.security.jwt.secret} (in production, the
     * {@code APP_SECURITY_JWT_SECRET} environment variable), the application refuses to start rather than accept tokens
     * signed with a key anyone can read.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String secret) {
        var bytes = secret.getBytes(StandardCharsets.UTF_8);
        // Nimbus would only reject a short key on the first request: fail at startup instead (RFC 7518, section 3.2)
        Assert.isTrue(bytes.length >= 32, "The JWT signing key must be at least 256 bits (32 bytes) for HS256");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(ROLE_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
