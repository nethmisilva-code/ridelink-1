package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: omitted fields keep their current value. */
public record UpdateProfileRequest(
        @Schema(example = "Nadeesha K. Perera") @Size(min = 2, max = 100) String fullName,
        @Schema(example = "+94770000000")
        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,15}$", message = "phone must contain 7 to 15 digits") String phone) {
}
