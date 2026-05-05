package com.eduai.mailservice.exception;

import com.eduai.mailservice.enums.mail.EmailStatus;
import lombok.Getter;

/**
 * Base exception for all email processing errors.
 */
@Getter
public class EmailException extends RuntimeException {

    private final String messageId;
    private final EmailStatus status;
    private final String provider;
    private final boolean retryable;

    public EmailException(String message) {
        super(message);
        this.messageId = null;
        this.status = EmailStatus.FAILED;
        this.provider = null;
        this.retryable = true;
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
        this.messageId = null;
        this.status = EmailStatus.FAILED;
        this.provider = null;
        this.retryable = true;
    }

    public EmailException(String message, String messageId, String provider, boolean retryable) {
        super(message);
        this.messageId = messageId;
        this.status = EmailStatus.FAILED;
        this.provider = provider;
        this.retryable = retryable;
    }

    public EmailException(String message, Throwable cause, String messageId, String provider, boolean retryable) {
        super(message, cause);
        this.messageId = messageId;
        this.status = EmailStatus.FAILED;
        this.provider = provider;
        this.retryable = retryable;
    }

    public static EmailException providerError(String provider, String detail) {
        return new EmailException("Provider [" + provider + "] error: " + detail, null, provider, true);
    }

    public static EmailException providerError(String provider, String detail, Throwable cause) {
        return new EmailException("Provider [" + provider + "] error: " + detail, cause, null, provider, true);
    }

    public static EmailException nonRetryable(String message) {
        return new EmailException(message, null, null, false);
    }

    public static EmailException templateError(String templateName, Throwable cause) {
        return new EmailException("Template rendering failed for: " + templateName, cause, null, null, false);
    }
}
