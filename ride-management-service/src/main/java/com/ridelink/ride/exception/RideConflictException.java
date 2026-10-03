package com.ridelink.ride.exception;

/** HTTP 409: a business rule blocks the request (for example one active ride per passenger). */
public class RideConflictException extends RuntimeException {
    public RideConflictException(String message) {
        super(message);
    }
}
