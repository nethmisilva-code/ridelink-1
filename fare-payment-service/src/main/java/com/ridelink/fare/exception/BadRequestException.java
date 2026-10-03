package com.ridelink.fare.exception;

/** HTTP 400, the request is invalid. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
