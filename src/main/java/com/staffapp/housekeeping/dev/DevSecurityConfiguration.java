package com.staffapp.housekeeping.dev;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Dev only: opens Swagger UI, the OpenAPI description and the token endpoint without authentication.
 */
@Profile("dev")
@Configuration
class DevSecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger.html", "/webjars/**", "/v3/api-docs/**", "/dev/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
