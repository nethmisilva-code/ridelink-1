package com.ridelink.fare.exception;

/** HTTP 404, the requested record does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
