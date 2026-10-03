package com.ridelink.ride.exception;

/** HTTP 403: the caller is not the passenger or the assigned driver of this ride. */
public class RideAccessDeniedException extends RuntimeException {
    public RideAccessDeniedException(String message) {
        super(message);
    }
}
