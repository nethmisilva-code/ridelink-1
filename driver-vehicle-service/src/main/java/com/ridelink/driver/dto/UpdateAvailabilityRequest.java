package com.ridelink.driver.dto;

import com.ridelink.driver.model.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "availability is required") AvailabilityStatus availability
) { }
