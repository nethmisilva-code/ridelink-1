package com.ridelink.account.dto;

import com.ridelink.account.model.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Nadeesha Perera")
        @NotBlank(message = "fullName is required") @Size(min = 2, max = 100) String fullName,

        @Schema(example = "nadeesha@ridelink.test")
        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address")
        @Size(max = 254) String email,

        @Schema(example = "+94771234567")
        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,15}$", message = "phone must contain 7 to 15 digits") String phone,

        @Schema(example = "Passw0rd!")
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters") String password,

        @Schema(example = "PASSENGER", allowableValues = { "PASSENGER", "DRIVER" })
        @NotNull(message = "role is required (PASSENGER or DRIVER)") Role role) {
}
