package com.eduai.mailservice.service.mail;

import com.eduai.mailservice.dto.request.ScheduleEmailRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;

/**
 * Scheduled email service contract.
 */
public interface SchedulerService {

    /**
     * Persist a scheduled email for future delivery.
     *
     * @param request schedule request
     * @return scheduled email response
     */
    EmailResponseDto scheduleEmail(ScheduleEmailRequestDto request);

    /**
     * Cancel a scheduled email by ID.
     *
     * @param messageId scheduled email ID
     * @return cancellation response
     */
    EmailResponseDto cancelScheduledEmail(String messageId);

    /**
     * Process all due scheduled emails. Called by the scheduler.
     */
    void processDueEmails();
}
