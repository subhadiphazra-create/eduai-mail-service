package com.eduai.mailservice.service.mail;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;

/**
 * Core email service interface.
 * Orchestrates validation → logging → queueing.
 */
public interface EmailService {

    /**
     * Send a single email asynchronously via the Redis queue.
     *
     * @param request email request
     * @return accepted response with message ID
     */
    EmailResponseDto sendEmail(EmailRequestDto request);

    /**
     * Get the current status of an email by message ID.
     *
     * @param messageId UUID of the email
     * @return current status response
     */
    EmailResponseDto getEmailStatus(String messageId);

    /**
     * Cancel a scheduled or queued email.
     *
     * @param messageId UUID of the email
     * @return cancellation response
     */
    EmailResponseDto cancelEmail(String messageId);
}
