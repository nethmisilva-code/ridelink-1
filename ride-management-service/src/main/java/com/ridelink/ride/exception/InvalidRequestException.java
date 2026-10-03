package com.ridelink.ride.exception;

/** HTTP 400: the request itself is invalid. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
