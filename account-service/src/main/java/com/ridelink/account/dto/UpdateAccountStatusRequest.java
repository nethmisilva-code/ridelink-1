package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @Schema(example = "ACTIVE") @NotNull(message = "status is required (ACTIVE, PENDING or INACTIVE)")
        AccountStatus status) {
}
