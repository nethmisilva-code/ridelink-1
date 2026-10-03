package com.ridelink.driver.exception;

/** A business rule blocks the request in the current state (HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
