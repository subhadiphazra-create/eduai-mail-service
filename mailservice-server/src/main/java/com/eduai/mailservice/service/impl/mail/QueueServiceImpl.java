package com.eduai.mailservice.service.impl.mail;

import com.eduai.mailservice.dto.request.BulkEmailRequestDto;
import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.response.BulkEmailResponseDto;
import com.eduai.mailservice.enums.mail.EmailType;
import com.eduai.mailservice.queue.EmailQueueProducer;
import com.eduai.mailservice.service.mail.EmailService;
import com.eduai.mailservice.service.mail.QueueService;
import com.eduai.mailservice.util.Constants;
import com.eduai.mailservice.util.EmailValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Queue service for bulk email operations and queue monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueServiceImpl implements QueueService {

    private final EmailService           emailService;
    private final EmailQueueProducer     queueProducer;
    private final EmailValidator         emailValidator;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final List<String> ALL_QUEUES = List.of(
            Constants.QUEUE_EMAIL_HIGH,
            Constants.QUEUE_EMAIL_NORMAL,
            Constants.QUEUE_EMAIL_LOW,
            Constants.QUEUE_EMAIL_RETRY,
            Constants.QUEUE_EMAIL_DLQ
    );

    @Override
    public BulkEmailResponseDto enqueueBulk(BulkEmailRequestDto request) {
        String batchId = UUID.randomUUID().toString();
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId() : UUID.randomUUID().toString();

        log.info("Processing bulk email [batchId={}] [recipients={}] [type={}]",
                batchId, request.getRecipients().size(), request.getEmailType());

        List<BulkEmailResponseDto.RecipientResult> results = new ArrayList<>();
        int queued = 0, failed = 0;

        for (BulkEmailRequestDto.BulkRecipient recipient : request.getRecipients()) {
            try {
                emailValidator.validateOrThrow(recipient.getEmail());

                // Merge global + per-recipient variables
                Map<String, Object> mergedVars = new HashMap<>();
                if (request.getGlobalTemplateVariables() != null) {
                    mergedVars.putAll(request.getGlobalTemplateVariables());
                }
                if (recipient.getTemplateVariables() != null) {
                    mergedVars.putAll(recipient.getTemplateVariables());
                }
                mergedVars.put("toName", recipient.getName());

                EmailRequestDto emailReq = EmailRequestDto.builder()
                        .fromEmail(request.getFromEmail())
                        .fromName(request.getFromName())
                        .toEmail(recipient.getEmail())
                        .toName(recipient.getName())
                        .subject(request.getSubject())
                        .emailType(request.getEmailType() != null ? request.getEmailType() : EmailType.BULK)
                        .templateVariables(mergedVars)
                        .htmlBody(request.getHtmlBody())
                        .appId(request.getAppId())
                        .correlationId(correlationId + "-" + queued)
                        .retryEnabled(request.isRetryEnabled())
                        .preferredProvider(request.getPreferredProvider())
                        .metadata(recipient.getMetadata())
                        .build();

                var response = emailService.sendEmail(emailReq);
                results.add(BulkEmailResponseDto.RecipientResult.builder()
                        .messageId(response.getMessageId())
                        .email(recipient.getEmail())
                        .queued(true)
                        .metadata(recipient.getMetadata())
                        .build());
                queued++;

            } catch (Exception ex) {
                log.warn("Failed to enqueue bulk email to [{}]: {}", recipient.getEmail(), ex.getMessage());
                results.add(BulkEmailResponseDto.RecipientResult.builder()
                        .email(recipient.getEmail())
                        .queued(false)
                        .reason(ex.getMessage())
                        .build());
                failed++;
            }
        }

        log.info("Bulk email batch [{}] complete: queued={} failed={}", batchId, queued, failed);

        return BulkEmailResponseDto.builder()
                .batchId(batchId)
                .totalRecipients(request.getRecipients().size())
                .queued(queued)
                .failed(failed)
                .accepted(queued > 0)
                .message("Bulk email batch processed: " + queued + " queued, " + failed + " failed")
                .results(results)
                .correlationId(correlationId)
                .build();
    }

    @Override
    public Map<String, Long> getQueueDepths() {
        Map<String, Long> depths = new LinkedHashMap<>();
        for (String queueKey : ALL_QUEUES) {
            depths.put(queueKey, queueProducer.getQueueDepth(queueKey));
        }
        return depths;
    }
}
