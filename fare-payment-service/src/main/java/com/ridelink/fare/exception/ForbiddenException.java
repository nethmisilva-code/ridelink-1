package com.ridelink.fare.exception;

/** HTTP 403, the caller may not access this record. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
