package com.eduai.mailservice.repository.mail;

import com.eduai.mailservice.entity.mail.ScheduledEmail;
import com.eduai.mailservice.enums.mail.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, String> {

    /**
     * Find scheduled emails due for processing.
     */
    @Query("SELECT s FROM ScheduledEmail s WHERE s.status = 'PENDING' " +
            "AND s.scheduledAt <= :now ORDER BY s.scheduledAt ASC")
    List<ScheduledEmail> findDueEmails(@Param("now") LocalDateTime now);

    List<ScheduledEmail> findByAppIdAndStatus(String appId, EmailStatus status);

    List<ScheduledEmail> findByStatus(EmailStatus status);

    @Query("SELECT s FROM ScheduledEmail s WHERE s.cronExpression IS NOT NULL " +
            "AND s.status = 'PENDING'")
    List<ScheduledEmail> findActiveRecurring();
}
