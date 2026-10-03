package com.ridelink.ride.exception;

/** HTTP 409: no eligible driver could be reserved. The ride stays REQUESTED. */
public class NoDriverAvailableException extends RuntimeException {
    public NoDriverAvailableException(String message) {
        super(message);
    }
}
