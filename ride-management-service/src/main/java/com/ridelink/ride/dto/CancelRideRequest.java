package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Who cancelled is taken from the caller's role, so only the reason is supplied. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRideRequest {

    @Schema(example = "Changed travel plans")
    @Size(max = 200)
    private String reason;
}
