package com.eduai.mailservice.entity.mail;

import com.eduai.mailservice.entity.base.BaseEntity;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.enums.mail.EmailType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persists scheduled email jobs for future or recurring delivery.
 */
@Entity
@Table(name = "scheduled_emails", indexes = {
        @Index(name = "idx_scheduled_status", columnList = "status"),
        @Index(name = "idx_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_scheduled_app_id", columnList = "app_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledEmail extends BaseEntity {

    @Column(name = "job_label")
    private String jobLabel;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(name = "to_name")
    private String toName;

    @Column(name = "from_email")
    private String fromEmail;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    /** JSON-serialized EmailRequestDto */
    @Column(name = "email_payload", columnDefinition = "TEXT", nullable = false)
    private String emailPayload;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /** Cron expression for recurring emails (null = one-shot) */
    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "occurrence_count")
    @Builder.Default
    private int occurrenceCount = 0;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "preferred_provider")
    private String preferredProvider;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Helper methods ──────────────────────────────────────────────────────

    public boolean isRecurring() {
        return cronExpression != null && !cronExpression.isBlank();
    }

    public boolean hasReachedMaxOccurrences() {
        return maxOccurrences != null && occurrenceCount >= maxOccurrences;
    }

    public void recordExecution() {
        this.occurrenceCount++;
        this.lastExecutedAt = LocalDateTime.now();
    }
}
