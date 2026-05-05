package com.eduai.mailservice.queue;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.enums.mail.EmailType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueMessage implements Serializable {

    private String messageId;
    private EmailRequestDto emailRequest;
    private EmailType emailType;

    @Builder.Default
    private int attemptCount = 0;

    @Builder.Default
    private int maxAttempts = 3;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enqueuedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processAfter;

    private String correlationId;
    private String appId;
    private String queueKey;
    private String lastError;

    // ── Helper methods ──────────────────────────────────────────────────────

    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void scheduleRetry(long delayMs) {
        this.processAfter = LocalDateTime.now().plusNanos(delayMs * 1_000_000L);
    }

    @JsonIgnore
    public boolean isReadyToProcess() {
        return processAfter == null || LocalDateTime.now().isAfter(processAfter);
    }

    public static QueueMessage of(String messageId, EmailRequestDto request) {
        return QueueMessage.builder()
                .messageId(messageId)
                .emailRequest(request)
                .emailType(request.getEmailType())
                .enqueuedAt(LocalDateTime.now())
                .correlationId(request.getCorrelationId())
                .appId(request.getAppId())
                .maxAttempts(request.getMaxRetries() + 1)
                .build();
    }
}