package com.eduai.mailservice.queue;

import com.eduai.mailservice.entity.mail.EmailLog;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.provider.EmailProvider;
import com.eduai.mailservice.repository.mail.EmailLogRepository;
import com.eduai.mailservice.service.mail.TemplateService;
import com.eduai.mailservice.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled consumer that polls Redis queues and dispatches emails via providers.
 *
 * <p>Processing order: HIGH → NORMAL → LOW → RETRY
 * <p>Each scheduling cycle processes up to {@code batchSize} messages per queue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueConsumer {

    private static final int BATCH_SIZE = 20;
    private static final long RETRY_BACKOFF_MS = 5_000L;

    private final RedisTemplate<String, Object>  redisTemplate;
    private final List<EmailProvider>            providers;
    private final TemplateService                templateService;
    private final EmailLogRepository             emailLogRepository;
    private final EmailQueueProducer             producer;

    // ── High-priority queue (OTP, transactional) ────────────────────────────

    @Scheduled(fixedDelay = 500)   // poll every 500 ms
    public void consumeHighPriority() {
        processQueue(Constants.QUEUE_EMAIL_HIGH);
    }

    // ── Normal-priority queue ────────────────────────────────────────────────

    @Scheduled(fixedDelay = 2000)
    public void consumeNormalPriority() {
        processQueue(Constants.QUEUE_EMAIL_NORMAL);
    }

    // ── Low-priority queue (bulk, promotional) ───────────────────────────────

    @Scheduled(fixedDelay = 5000)
    public void consumeLowPriority() {
        processQueue(Constants.QUEUE_EMAIL_LOW);
    }

    // ── Retry queue ──────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 30000)  // every 30 s
    public void consumeRetryQueue() {
        processQueue(Constants.QUEUE_EMAIL_RETRY);
    }

    // ── Core processing ──────────────────────────────────────────────────────


    private void processQueue(String queueKey) {
        int processed = 0;
        while (processed < BATCH_SIZE) {
            Object raw = redisTemplate.opsForList().leftPop(queueKey);
            if (raw == null) break;

            QueueMessage message = (QueueMessage) raw;

            // Back-off: re-queue if not ready yet, but CONTINUE to next message
            if (!message.isReadyToProcess()) {
                redisTemplate.opsForList().rightPush(queueKey, message);
                processed++;  // ← count it and continue, don't break
                continue;     // ← try next message instead of stopping
            }

            processMessage(message);
            processed++;
        }
    }

    private void processMessage(QueueMessage message) {
        log.info("Processing email [id={}] [type={}] [attempt={}/{}]",
                message.getMessageId(),
                message.getEmailType(),
                message.getAttemptCount() + 1,
                message.getMaxAttempts());

        Optional<EmailLog> logOpt = emailLogRepository.findById(message.getMessageId());
        if (logOpt.isEmpty()) {
            log.warn("EmailLog not found for messageId={}", message.getMessageId());
            return;
        }
        EmailLog emailLog = logOpt.get();
        emailLog.setStatus(EmailStatus.PROCESSING);
        emailLogRepository.save(emailLog);

        try {
            // Render template if needed
            String htmlBody = prepareHtmlBody(message, emailLog);

            // Dispatch via provider chain
            sendViaProvider(message, emailLog, htmlBody);

        } catch (Exception ex) {
            handleFailure(message, emailLog, ex);
        }
    }

    private String prepareHtmlBody(QueueMessage message, EmailLog emailLog) {
        var request = message.getEmailRequest();
        if (request.getHtmlBody() != null && !request.getHtmlBody().isBlank()) {
            return request.getHtmlBody();
        }
        if (request.getEmailType().requiresTemplate()) {
            return templateService.render(
                    request.getEmailType().getTemplateName(),
                    request.getTemplateVariables());
        }
        return request.getTextBody();
    }

    private void sendViaProvider(QueueMessage message, EmailLog emailLog, String htmlBody) {
        var request = message.getEmailRequest();
        String preferredProvider = request.getPreferredProvider();

        // Build ordered provider list: preferred first, then others as fallback
        List<EmailProvider> orderedProviders = providers.stream()
                .sorted((a, b) -> {
                    if (a.getProviderName().equalsIgnoreCase(preferredProvider)) return -1;
                    if (b.getProviderName().equalsIgnoreCase(preferredProvider)) return 1;
                    return Integer.compare(a.getPriority(), b.getPriority());
                })
                .toList();

        Exception lastException = null;
        for (EmailProvider provider : orderedProviders) {
            if (!provider.isAvailable()) continue;
            try {
                String providerMessageId = provider.send(request, htmlBody);
                emailLog.markSent(providerMessageId, provider.getProviderName());
                emailLogRepository.save(emailLog);
                log.info("Email sent [id={}] via [{}] providerMsgId={}",
                        message.getMessageId(), provider.getProviderName(), providerMessageId);
                return;
            } catch (Exception ex) {
                log.warn("Provider [{}] failed for [id={}]: {}",
                        provider.getProviderName(), message.getMessageId(), ex.getMessage());
                lastException = ex;
            }
        }

        // All providers failed
        throw new RuntimeException("All providers failed. Last error: " +
                (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }

    private void handleFailure(QueueMessage message, EmailLog emailLog, Exception ex) {
        log.error("Email processing failed [id={}] [attempt={}]: {}",
                message.getMessageId(), message.getAttemptCount() + 1, ex.getMessage());

        emailLog.incrementRetry();
        message.setLastError(ex.getMessage());

        if (message.canRetry()) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLogRepository.save(emailLog);
            message.incrementAttempt();  // ← increment once here only
            message.scheduleRetry(RETRY_BACKOFF_MS * message.getAttemptCount());
            producer.enqueueRetry(message);  // ← enqueueRetry no longer increments
            log.warn("Message [id={}] enqueued for retry (attempt {})",
                    message.getMessageId(), message.getAttemptCount());
        } else {
            emailLog.markFailed(ex.getMessage());
            emailLogRepository.save(emailLog);
            producer.enqueueDlq(message);
            log.error("Email permanently failed [id={}] after {} attempts",
                    message.getMessageId(), message.getAttemptCount());
        }
    }
}
