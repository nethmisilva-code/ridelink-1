package com.ridelink.driver.exception;

/** A service this one depends on (Account Service) could not be reached (HTTP 503). */
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message) {
        super(message);
    }
}
