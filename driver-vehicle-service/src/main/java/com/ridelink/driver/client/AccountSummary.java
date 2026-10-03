package com.ridelink.driver.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The slice of an Account that this service needs. Deliberately minimal:
 * we never mirror the Account Service's full model, only the fields
 * required to validate that a driver profile may be created.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountSummary(
        String id,
        String email,
        String role,
        String status
) { }
