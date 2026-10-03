package com.ridelink.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    OpenAPI accountServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("RideLink Account Service").version("1.0.0")
                        .description("Passenger and driver registration, login and JWT issuance, roles, "
                                + "profiles and account status management. Log in, copy the token and press "
                                + "Authorize."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Token returned by POST /api/v1/auth/login")));
    }
}
