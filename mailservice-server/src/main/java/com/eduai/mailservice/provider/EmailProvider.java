package com.eduai.mailservice.provider;

import com.eduai.mailservice.dto.request.EmailRequestDto;

/**
 * Strategy interface for email delivery providers.
 *
 * <p>Implementations: {@link SmtpProvider}, {@link ResendProvider}, {@link SendGridProvider}
 *
 * <p>The consumer selects providers by priority + availability, with automatic fallback.
 */
public interface EmailProvider {

    /**
     * Send an email using this provider.
     *
     * @param request  the email request (recipient, subject, metadata)
     * @param htmlBody the rendered HTML body (may be null for text-only emails)
     * @return provider-assigned message ID for delivery tracking
     * @throws com.eduai.mailservice.exception.EmailException on delivery failure
     */
    String send(EmailRequestDto request, String htmlBody);

    /**
     * Provider name key (e.g., "smtp", "resend", "sendgrid").
     */
    String getProviderName();

    /**
     * Provider priority order. Lower = higher priority.
     * Used when no preferred provider is specified.
     */
    int getPriority();

    /**
     * Whether this provider is currently available/healthy.
     * Return false to skip during provider selection.
     */
    boolean isAvailable();
}
