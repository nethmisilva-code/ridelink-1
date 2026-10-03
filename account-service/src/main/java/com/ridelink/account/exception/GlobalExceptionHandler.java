package com.ridelink.account.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Account Service raises ResponseStatusException for its business errors
 * (404, 409, 401, 403), which BaseExceptionHandler already maps to ApiError.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {
}
