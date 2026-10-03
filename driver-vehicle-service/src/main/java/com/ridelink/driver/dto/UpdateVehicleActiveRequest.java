package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateVehicleActiveRequest(
        @NotNull(message = "active is required") Boolean active
) { }
