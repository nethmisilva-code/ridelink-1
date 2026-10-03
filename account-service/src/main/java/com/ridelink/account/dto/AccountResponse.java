package com.ridelink.account.dto;

import java.time.Instant;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;

/** Public view of an account. The password hash is never exposed. */
public record AccountResponse(
        String id,
        String fullName,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id(), account.fullName(), account.email(), account.phone(),
                account.effectiveRole(), account.effectiveStatus(), account.createdAt());
    }
}
