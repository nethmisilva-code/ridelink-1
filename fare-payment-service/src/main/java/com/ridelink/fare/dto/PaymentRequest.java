package com.ridelink.fare.dto;

import com.ridelink.fare.model.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @Schema(example = "66f1a2b3c4d5e6f7a8b9c0d1")
        @NotBlank(message = "rideId is required") String rideId,
        @Schema(example = "CASH")
        @NotNull(message = "paymentMethod is required (CASH, CARD or WALLET)") PaymentMethod paymentMethod,
        @Schema(description = "Fictional test card, CARD only. Numbers ending in 0002 are declined.",
                example = "4242 4242 4242 4242", nullable = true)
        @Pattern(regexp = "^[0-9 ]{12,23}$", message = "cardNumber must contain 12 to 19 digits")
        String cardNumber) {
}
