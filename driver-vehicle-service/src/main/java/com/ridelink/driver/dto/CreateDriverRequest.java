package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDriverRequest(
        @Schema(description = "Optional for drivers (defaults to your own account id). Required for ADMIN.",
                nullable = true)
        String accountId,
        @Schema(example = "B1234567")
        @NotBlank(message = "licenseNumber is required") @Size(max = 20) String licenseNumber,
        @Schema(example = "Negombo")
        @NotBlank(message = "serviceArea is required") @Size(max = 50) String serviceArea
) { }
