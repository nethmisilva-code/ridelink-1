package com.ridelink.driver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateVehicleRequest(
        @NotBlank(message = "make is required") String make,
        @NotBlank(message = "model is required") String model,
        @Min(value = 1990, message = "year must be 1990 or later") int year,
        @NotBlank(message = "colour is required") String colour,
        @Min(value = 1, message = "seats must be at least 1")
        @Max(value = 20, message = "seats must be at most 20") int seats
) { }
