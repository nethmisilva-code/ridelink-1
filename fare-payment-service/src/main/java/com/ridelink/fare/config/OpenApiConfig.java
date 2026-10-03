package com.ridelink.fare.config;

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
    public OpenAPI fareServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("RideLink Fare & Payment Service").version("1.0.0")
                        .description("Fare estimation and final fare calculation with a documented rule, simulated "
                                + "payments and receipts. Rule: fare = base + perKm x distance + perMinute x minutes "
                                + "(LKR). Test cards ending in 0002 are always declined. Use a token from Account "
                                + "Service (port 8081) and press Authorize."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
