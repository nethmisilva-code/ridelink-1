package com.ridelink.account.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Account aggregate owned exclusively by Account Service. Other services only
 * ever hold the account id as a stable identifier.
 *
 * @param passwordHash BCrypt hash. Stored in the "password" field so documents
 *                     written by earlier builds remain readable.
 */
@Document(collection = "accounts")
public record Account(
        @Id String id,
        @Indexed(unique = true) String email,
        String fullName,
        String phone,
        @Field("password") String passwordHash,
        Role role,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Role effectiveRole() {
        return role == null ? null : role.normalised();
    }

    /** Older documents may not have a status; derive the same default used at registration. */
    public AccountStatus effectiveStatus() {
        if (status != null) {
            return status;
        }
        return effectiveRole() == Role.DRIVER ? AccountStatus.PENDING : AccountStatus.ACTIVE;
    }

    public Account withProfile(String newFullName, String newPhone, Instant now) {
        return new Account(id, email, newFullName, newPhone, passwordHash, role, status, createdAt, now);
    }

    public Account withPasswordHash(String newHash, Instant now) {
        return new Account(id, email, fullName, phone, newHash, role, status, createdAt, now);
    }

    public Account withStatus(AccountStatus newStatus, Instant now) {
        return new Account(id, email, fullName, phone, passwordHash, role, newStatus, createdAt, now);
    }
}
