package com.eduai.mailservice.service.impl.mail;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;
import com.eduai.mailservice.entity.mail.EmailLog;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.exception.EmailException;
import com.eduai.mailservice.queue.EmailQueueProducer;
import com.eduai.mailservice.queue.QueueMessage;
import com.eduai.mailservice.repository.mail.EmailLogRepository;
import com.eduai.mailservice.service.mail.EmailService;
import com.eduai.mailservice.util.EmailValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core email service implementation.
 * Validates → persists EmailLog → enqueues for async processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailLogRepository  emailLogRepository;
    private final EmailQueueProducer  queueProducer;
    private final EmailValidator      emailValidator;
    private final ObjectMapper        objectMapper;

    @Override
    @Transactional
    public EmailResponseDto sendEmail(EmailRequestDto request) {
        // Validate
        emailValidator.validateOrThrow(request.getToEmail());

        String messageId = UUID.randomUUID().toString();
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : UUID.randomUUID().toString();
        request.setCorrelationId(correlationId);

        log.info("Accepting email [id={}] [type={}] [to={}] [correlationId={}]",
                messageId, request.getEmailType(),
                emailValidator.mask(request.getToEmail()), correlationId);

        // Persist audit log
        EmailLog emailLog = EmailLog.builder()
//                .id(messageId)
                .toEmail(request.getToEmail())
                .toName(request.getToName())
                .fromEmail(request.getFromEmail())
                .fromName(request.getFromName())
                .subject(request.getSubject())
                .emailType(request.getEmailType())
                .status(EmailStatus.QUEUED)
                .correlationId(correlationId)
                .appId(request.getAppId())
                .retryEnabled(request.isRetryEnabled())
                .maxRetries(request.getMaxRetries())
                .metadata(serializeMetadata(request.getMetadata()))
                .build();

        emailLog.setId(messageId);
        emailLogRepository.save(emailLog);

        // Enqueue
        QueueMessage message = QueueMessage.of(messageId, request);
        queueProducer.enqueue(message);

        return EmailResponseDto.accepted(messageId, request.getToEmail(),
                request.getEmailType(), correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailResponseDto getEmailStatus(String messageId) {
        EmailLog log = emailLogRepository.findById(messageId)
                .orElseThrow(() -> EmailException.nonRetryable("Email not found: " + messageId));

        return EmailResponseDto.builder()
                .messageId(log.getId())
                .status(log.getStatus())
                .emailType(log.getEmailType())
                .toEmail(log.getToEmail())
                .providerMessageId(log.getProviderMessageId())
                .providerUsed(log.getProviderUsed())
                .sentAt(log.getSentAt())
                .acceptedAt(log.getCreatedAt())
                .correlationId(log.getCorrelationId())
                .message("Status retrieved successfully")
                .build();
    }

    @Override
    @Transactional
    public EmailResponseDto cancelEmail(String messageId) {
        EmailLog emailLog = emailLogRepository.findById(messageId)
                .orElseThrow(() -> EmailException.nonRetryable("Email not found: " + messageId));

        if (emailLog.getStatus().isTerminal()) {
            throw EmailException.nonRetryable(
                    "Cannot cancel email in terminal state: " + emailLog.getStatus());
        }

        emailLog.setStatus(EmailStatus.CANCELLED);
        emailLogRepository.save(emailLog);

        return EmailResponseDto.builder()
                .messageId(messageId)
                .status(EmailStatus.CANCELLED)
                .message("Email cancelled successfully")
                .build();
    }

    private String serializeMetadata(java.util.Map<String, String> metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
