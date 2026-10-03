package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "nadeesha@ridelink.test") @NotBlank @Email String email,
        @Schema(example = "Passw0rd!") @NotBlank String password) {
}
