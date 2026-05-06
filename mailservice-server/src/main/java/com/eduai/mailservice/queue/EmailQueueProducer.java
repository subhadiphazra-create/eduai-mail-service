package com.eduai.mailservice.queue;

import com.eduai.mailservice.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes email messages onto priority-based Redis queues.
 *
 * <p>Queue strategy:
 * <ul>
 *   <li>HIGH  — OTP, password reset, transactional (priority 1-2)</li>
 *   <li>NORMAL — welcome, reminder, update, notification (priority 3)</li>
 *   <li>LOW   — promotional, bulk (priority 4-5)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Enqueue a message onto the appropriate priority queue.
     *
     * @param message the queue message
     */
    public void enqueue(QueueMessage message) {
        String queueKey = resolveQueue(message);
        message.setQueueKey(queueKey);

        try {
            redisTemplate.opsForList().rightPush(queueKey, message);
            log.info("Enqueued message [id={}] [type={}] [queue={}] [correlationId={}]",
                    message.getMessageId(),
                    message.getEmailType(),
                    queueKey,
                    message.getCorrelationId());
        } catch (Exception ex) {
            log.error("Failed to enqueue message [id={}]: {}",
                    message.getMessageId(), ex.getMessage(), ex);
            throw new RuntimeException("Queue unavailable. Message could not be enqueued.", ex);
        }
    }

    /**
     * Push a message to the Dead Letter Queue after all retries exhausted.
     */
    public void enqueueDlq(QueueMessage message) {
        try {
            redisTemplate.opsForList().rightPush(Constants.QUEUE_EMAIL_DLQ, message);
            log.warn("Message [id={}] moved to DLQ after {} attempts",
                    message.getMessageId(), message.getAttemptCount());
        } catch (Exception ex) {
            log.error("Failed to enqueue to DLQ [id={}]: {}",
                    message.getMessageId(), ex.getMessage(), ex);
        }
    }

    /**
     * Re-queue a message for retry (pushed to retry queue).
     */
    public void enqueueRetry(QueueMessage message) {
        try {
            // REMOVED: message.incrementAttempt() — already done in handleFailure
            redisTemplate.opsForList().rightPush(Constants.QUEUE_EMAIL_RETRY, message);
            log.info("Message [id={}] enqueued for retry (attempt {})",
                    message.getMessageId(), message.getAttemptCount());
        } catch (Exception ex) {
            log.error("Failed to enqueue retry [id={}]: {}",
                    message.getMessageId(), ex.getMessage(), ex);
        }
    }

    /**
     * Get queue depth for monitoring.
     */
    public long getQueueDepth(String queueKey) {
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0L;
    }

    private String resolveQueue(QueueMessage message) {
        if (message.getEmailType() == null) return Constants.QUEUE_EMAIL_NORMAL;
        return switch (message.getEmailType()) {
            case OTP, PASSWORD_RESET, TRANSACTIONAL -> Constants.QUEUE_EMAIL_HIGH;
            case PROMOTIONAL, BULK                  -> Constants.QUEUE_EMAIL_LOW;
            default                                  -> Constants.QUEUE_EMAIL_NORMAL;
        };
    }
}
