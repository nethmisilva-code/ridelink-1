package com.ridelink.ride.config;

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
    public OpenAPI rideServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("RideLink Ride Management Service").version("1.0.0")
                        .description("Ride requests, nearest-driver assignment and the ride lifecycle "
                                + "REQUESTED > ASSIGNED > ACCEPTED > IN_PROGRESS > COMPLETED (or CANCELLED). "
                                + "Use a token from Account Service (port 8081) and press Authorize."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
