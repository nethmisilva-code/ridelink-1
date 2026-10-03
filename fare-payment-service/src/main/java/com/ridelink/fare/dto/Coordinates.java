package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record Coordinates(
        @Schema(example = "7.2087")
        @NotNull(message = "latitude is required") @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Schema(example = "79.8358")
        @NotNull(message = "longitude is required") @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}
