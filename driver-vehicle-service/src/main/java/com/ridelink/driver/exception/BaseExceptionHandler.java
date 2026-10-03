package com.ridelink.driver.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Framework-level error mappings shared by every RideLink service. Each service
 * extends this in its own @RestControllerAdvice and adds its domain exceptions.
 * Unexpected errors are logged and returned without internal details.
 */
public abstract class BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed for one or more request parameters", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Request body is missing, is not valid JSON, or contains a value of the wrong type", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", request,
                null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing required header '" + ex.getHeaderName() + "'", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + ex.getMethod() + " is not supported here",
                request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint matches this path", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode code = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(code.value());
        String message = ex.getReason() != null ? ex.getReason()
                : (status != null ? status.getReasonPhrase() : "Error");
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, message, request, null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "A record with the same unique value already exists", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "The record was changed by another request. Reload it and try again",
                request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    protected ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
            Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
