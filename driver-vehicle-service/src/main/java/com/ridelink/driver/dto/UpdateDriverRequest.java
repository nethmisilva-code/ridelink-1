package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Partial update of the operational profile: omitted fields keep their value. */
public record UpdateDriverRequest(
        @Schema(example = "B7654321") @Size(min = 1, max = 20) String licenseNumber,
        @Schema(example = "Colombo") @Size(min = 1, max = 50) String serviceArea
) { }
