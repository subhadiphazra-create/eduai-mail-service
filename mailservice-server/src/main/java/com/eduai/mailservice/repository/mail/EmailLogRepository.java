package com.eduai.mailservice.repository.mail;

import com.eduai.mailservice.entity.mail.EmailLog;
import com.eduai.mailservice.enums.mail.EmailStatus;
import com.eduai.mailservice.enums.mail.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, String> {

    Optional<EmailLog> findByCorrelationId(String correlationId);

    Page<EmailLog> findByToEmail(String toEmail, Pageable pageable);

    Page<EmailLog> findByAppId(String appId, Pageable pageable);

    Page<EmailLog> findByStatus(EmailStatus status, Pageable pageable);

    Page<EmailLog> findByEmailType(EmailType emailType, Pageable pageable);

    List<EmailLog> findByStatusAndRetryCountLessThanAndRetryEnabledTrue(
            EmailStatus status, int maxRetries);

    @Query("SELECT e FROM EmailLog e WHERE e.status = :status AND e.retryEnabled = true " +
            "AND e.retryCount < e.maxRetries AND e.failedAt < :before")
    List<EmailLog> findRetryableEmails(
            @Param("status") EmailStatus status,
            @Param("before") LocalDateTime before);

    @Modifying
    @Query("UPDATE EmailLog e SET e.status = :status, e.updatedAt = :now WHERE e.id = :id")
    void updateStatus(@Param("id") String id,
                      @Param("status") EmailStatus status,
                      @Param("now") LocalDateTime now);

    long countByAppIdAndCreatedAtBetween(String appId, LocalDateTime from, LocalDateTime to);

    long countByStatusAndCreatedAtBetween(EmailStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.toEmail = :email AND e.emailType = 'OTP' " +
            "AND e.createdAt > :since")
    long countOtpSentToEmail(@Param("email") String email, @Param("since") LocalDateTime since);
}
