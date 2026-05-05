package com.eduai.mailservice.entity.mail;

import com.eduai.mailservice.entity.base.BaseEntity;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.enums.mail.EmailType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log of every email processed by the service.
 * Enables status tracking, retry management and analytics.
 */
@Entity
@Table(name = "email_logs", indexes = {
        @Index(name = "idx_email_log_to_email", columnList = "to_email"),
        @Index(name = "idx_email_log_status", columnList = "status"),
        @Index(name = "idx_email_log_type", columnList = "email_type"),
        @Index(name = "idx_email_log_app_id", columnList = "app_id"),
        @Index(name = "idx_email_log_correlation", columnList = "correlation_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog extends BaseEntity {

    @Column(name = "from_email")
    private String fromEmail;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(name = "to_name")
    private String toName;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;

    @Column(name = "provider_used")
    private String providerUsed;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "retry_enabled")
    @Builder.Default
    private boolean retryEnabled = true;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // JSON serialized Map<String,String>

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "scheduled_email_id")
    private String scheduledEmailId;

    // ── Helper methods ──────────────────────────────────────────────────────

    public boolean canRetry() {
        return retryEnabled && retryCount < maxRetries && !status.isTerminal();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markSent(String providerMessageId, String provider) {
        this.status = EmailStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.providerUsed = provider;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = canRetry() ? EmailStatus.FAILED : EmailStatus.PERMANENTLY_FAILED;
        this.errorMessage = errorMessage;
        this.failedAt = LocalDateTime.now();
    }
}
