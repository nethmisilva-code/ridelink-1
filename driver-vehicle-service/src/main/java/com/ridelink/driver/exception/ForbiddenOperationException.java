package com.ridelink.driver.exception;

/** The caller is authenticated but may not act on this driver or vehicle (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
