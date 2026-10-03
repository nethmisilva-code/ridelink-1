package com.ridelink.ride.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Domain exceptions of this service; framework errors are handled by BaseExceptionHandler. */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(RideNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler({ InvalidStatusTransitionException.class, RideConflictException.class,
            NoDriverAvailableException.class })
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(RideAccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(RideAccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiError> handleDownstream(DownstreamServiceException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, null);
    }
}
