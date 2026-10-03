package com.ridelink.driver.dto;

import com.ridelink.driver.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
        @NotBlank(message = "driverId is required") String driverId,
        @Schema(example = "CAB-1234") @NotBlank(message = "plateNumber is required") @Size(max = 15) String plateNumber,
        @Schema(example = "Toyota") @NotBlank(message = "make is required") String make,
        @Schema(example = "Prius") @NotBlank(message = "model is required") String model,
        @Schema(example = "2019") @Min(value = 1990, message = "year must be 1990 or later") int year,
        @Schema(example = "White") @NotBlank(message = "colour is required") String colour,
        @Schema(example = "SEDAN") @NotNull(message = "vehicleType is required") VehicleType vehicleType,
        @Schema(example = "4")
        @Min(value = 1, message = "seats must be at least 1")
        @Max(value = 20, message = "seats must be at most 20") int seats
) { }
