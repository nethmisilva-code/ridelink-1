package com.ridelink.fare.model;

/** PENDING once the final fare is known, SUCCESS when paid, FAILED after a declined attempt (can be retried). */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
