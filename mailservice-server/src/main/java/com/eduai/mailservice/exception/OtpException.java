package com.eduai.mailservice.exception;

import lombok.Getter;

/**
 * Exception for OTP-specific errors: rate limiting, expiry, generation failures.
 */
@Getter
public class OtpException extends RuntimeException {

    public enum OtpErrorCode {
        RATE_LIMIT_EXCEEDED,
        GENERATION_FAILED,
        INVALID_EMAIL,
        SEND_FAILED
    }

    private final OtpErrorCode errorCode;
    private final String email;

    public OtpException(String message, OtpErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.email = null;
    }

    public OtpException(String message, OtpErrorCode errorCode, String email) {
        super(message);
        this.errorCode = errorCode;
        this.email = email;
    }

    public OtpException(String message, OtpErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.email = null;
    }

    public static OtpException rateLimitExceeded(String email) {
        return new OtpException(
                "OTP rate limit exceeded for: " + email,
                OtpErrorCode.RATE_LIMIT_EXCEEDED, email);
    }

    public static OtpException sendFailed(String email, Throwable cause) {
        return new OtpException(
                "Failed to send OTP to: " + email,
                OtpErrorCode.SEND_FAILED, cause);
    }
}
