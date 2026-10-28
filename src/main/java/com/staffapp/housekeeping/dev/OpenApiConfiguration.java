package com.staffapp.housekeeping.dev;

import com.staffapp.housekeeping.tenant.TenantFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dev only: describes the two headers every API call needs, so that Swagger UI sends them.
 */
@Profile("dev")
@Configuration
class OpenApiConfiguration {

    private static final String BEARER = "bearer";
    private static final String ESTABLISHMENT = "establishment";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Housekeeping API"))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(ESTABLISHMENT, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(TenantFilter.ESTABLISHMENT_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER).addList(ESTABLISHMENT));
    }
}
