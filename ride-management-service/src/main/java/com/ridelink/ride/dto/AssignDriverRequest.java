package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body for driver assignment. Leave it out (or leave driverId empty)
 * to auto-assign the nearest eligible driver. ADMIN may name a specific
 * driver profile id (manual dispatch); it must still be eligible.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDriverRequest {

    @Schema(description = "ADMIN only: a specific driver profile id", nullable = true)
    private String driverId;
}
