package com.ridelink.driver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI driverServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink Driver & Vehicle Service")
                        .version("1.0.0")
                        .description("Driver operational profiles, vehicles, availability, simulated location "
                                + "and eligible-driver lookup. Use a token from Account Service "
                                + "(POST /api/v1/auth/login on port 8081) and press Authorize."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
