package com.eduai.mailservice.exception;

import com.eduai.mailservice.dto.response.EmailResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for all REST endpoints.
 * Returns consistent JSON error envelopes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation Errors ───────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe ->
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed. Check 'errors' for details.")
                .path(request.getDescription(false))
                .errors(errors)
                .build();

        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(response);
    }

    // ── Email Errors ────────────────────────────────────────────────────────

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ErrorResponse> handleEmailException(EmailException ex, WebRequest request) {
        log.error("Email processing error [provider={}] [retryable={}]: {}",
                ex.getProvider(), ex.isRetryable(), ex.getMessage(), ex);

        HttpStatus status = ex.isRetryable()
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.UNPROCESSABLE_ENTITY;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("Email Processing Error")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    // ── OTP Errors ──────────────────────────────────────────────────────────

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> handleOtpException(OtpException ex, WebRequest request) {
        log.warn("OTP error [code={}]: {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = ex.getErrorCode() == OtpException.OtpErrorCode.RATE_LIMIT_EXCEEDED
                ? HttpStatus.TOO_MANY_REQUESTS
                : HttpStatus.BAD_REQUEST;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("OTP Error")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    // ── Illegal Arguments ───────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    // ── Catch-all ───────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.internalServerError().body(response);
    }

    // ── Error Response DTO ──────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> errors;
    }
}
