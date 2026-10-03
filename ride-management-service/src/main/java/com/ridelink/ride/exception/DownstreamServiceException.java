package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;

/**
 * A dependency (Driver &amp; Vehicle or Fare &amp; Payment) failed.
 * 503 when it is unreachable, 502 when it answered with an unexpected error.
 */
public class DownstreamServiceException extends RuntimeException {

    private final HttpStatus status;

    public DownstreamServiceException(String message) {
        this(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public DownstreamServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
