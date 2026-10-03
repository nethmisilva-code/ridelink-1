package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String accountId,
        String role,
        AccountStatus status) {
}
