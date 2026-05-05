package com.eduai.mailservice.service.impl.mail;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.request.ScheduleEmailRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;
import com.eduai.mailservice.entity.mail.ScheduledEmail;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.exception.EmailException;
import com.eduai.mailservice.repository.mail.ScheduledEmailRepository;
import com.eduai.mailservice.service.mail.EmailService;
import com.eduai.mailservice.service.mail.SchedulerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduler service implementation.
 * Persists scheduled emails and dispatches due emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {

    private final ScheduledEmailRepository scheduledEmailRepository;
    private final EmailService             emailService;
    private final ObjectMapper             objectMapper;

    @Override
    @Transactional
    public EmailResponseDto scheduleEmail(ScheduleEmailRequestDto request) {
        String messageId = UUID.randomUUID().toString();
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId() : UUID.randomUUID().toString();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(request.getEmailRequest());
        } catch (JsonProcessingException ex) {
            throw EmailException.nonRetryable("Failed to serialize email request");
        }

        ScheduledEmail scheduled = ScheduledEmail.builder()
//                .id(messageId)
                .jobLabel(request.getJobLabel())
                .toEmail(request.getEmailRequest().getToEmail())
                .toName(request.getEmailRequest().getToName())
                .subject(request.getEmailRequest().getSubject())
                .emailType(request.getEmailRequest().getEmailType())
                .emailPayload(payload)
                .scheduledAt(request.getScheduledAt())
                .cronExpression(request.getCronExpression())
                .maxOccurrences(request.getMaxOccurrences())
                .status(EmailStatus.PENDING)
                .correlationId(correlationId)
                .appId(request.getAppId())
                .preferredProvider(request.getEmailRequest().getPreferredProvider())
                .build();

        scheduled.setId(messageId);

        scheduledEmailRepository.save(scheduled);

        log.info("Scheduled email [id={}] for [{}] at [{}]",
                messageId, request.getEmailRequest().getToEmail(), request.getScheduledAt());

        return EmailResponseDto.scheduled(messageId,
                request.getEmailRequest().getToEmail(),
                request.getEmailRequest().getEmailType(),
                request.getScheduledAt());
    }

    @Override
    @Transactional
    public EmailResponseDto cancelScheduledEmail(String messageId) {
        ScheduledEmail scheduled = scheduledEmailRepository.findById(messageId)
                .orElseThrow(() -> EmailException.nonRetryable("Scheduled email not found: " + messageId));

        if (scheduled.getStatus().isTerminal()) {
            throw EmailException.nonRetryable("Cannot cancel email in state: " + scheduled.getStatus());
        }

        scheduled.setStatus(EmailStatus.CANCELLED);
        scheduledEmailRepository.save(scheduled);

        log.info("Cancelled scheduled email [id={}]", messageId);
        return EmailResponseDto.builder()
                .messageId(messageId)
                .status(EmailStatus.CANCELLED)
                .message("Scheduled email cancelled")
                .build();
    }

    @Override
    @Transactional
    public void processDueEmails() {
        List<ScheduledEmail> dueEmails = scheduledEmailRepository
                .findDueEmails(LocalDateTime.now());

        if (dueEmails.isEmpty()) return;

        log.info("Processing {} due scheduled emails", dueEmails.size());

        for (ScheduledEmail scheduled : dueEmails) {
            try {
                processScheduledEmail(scheduled);
            } catch (Exception ex) {
                log.error("Failed to process scheduled email [id={}]: {}",
                        scheduled.getId(), ex.getMessage(), ex);
                scheduled.setStatus(EmailStatus.FAILED);
                scheduled.setErrorMessage(ex.getMessage());
                scheduledEmailRepository.save(scheduled);
            }
        }
    }

    private void processScheduledEmail(ScheduledEmail scheduled) throws JsonProcessingException {
        EmailRequestDto emailRequest = objectMapper.readValue(
                scheduled.getEmailPayload(), EmailRequestDto.class);
        emailRequest.setCorrelationId(scheduled.getCorrelationId());

        emailService.sendEmail(emailRequest);
        scheduled.recordExecution();

        if (scheduled.isRecurring() && !scheduled.hasReachedMaxOccurrences()) {
            // Keep PENDING for cron-based recurrence; next execution computed by scheduler
            scheduled.setStatus(EmailStatus.PENDING);
            // Simple next-run: scheduler will pick it up next cycle with cronExpression support
        } else {
            scheduled.setStatus(EmailStatus.SENT);
        }

        scheduledEmailRepository.save(scheduled);
        log.info("Dispatched scheduled email [id={}] occurrence={}",
                scheduled.getId(), scheduled.getOccurrenceCount());
    }
}
