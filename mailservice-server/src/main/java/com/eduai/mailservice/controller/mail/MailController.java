package com.eduai.mailservice.controller.mail;

import com.eduai.mailservice.api.mail.MailApi;
import com.eduai.mailservice.dto.request.BulkEmailRequestDto;
import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.request.OtpRequestDto;
import com.eduai.mailservice.dto.request.ScheduleEmailRequestDto;
import com.eduai.mailservice.dto.response.BulkEmailResponseDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;
import com.eduai.mailservice.dto.response.OtpResponseDto;
import com.eduai.mailservice.service.mail.EmailService;
import com.eduai.mailservice.service.mail.OtpService;
import com.eduai.mailservice.service.mail.QueueService;
import com.eduai.mailservice.service.mail.SchedulerService;
import com.eduai.mailservice.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller implementing the MailApi contract.
 *
 * <p>All endpoints:
 * <ul>
 *   <li>POST  /api/v1/mail/send         — send single email</li>
 *   <li>POST  /api/v1/mail/send/otp     — send OTP email</li>
 *   <li>POST  /api/v1/mail/send/bulk    — send bulk email</li>
 *   <li>POST  /api/v1/mail/schedule     — schedule email</li>
 *   <li>GET   /api/v1/mail/status/{id}  — get email status</li>
 *   <li>DELETE /api/v1/mail/schedule/{id} — cancel scheduled</li>
 *   <li>GET   /api/v1/mail/ping         — health check</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MailController implements MailApi {

    private final EmailService    emailService;
    private final OtpService      otpService;
    private final QueueService    queueService;
    private final SchedulerService schedulerService;

    @Override
    public ResponseEntity<EmailResponseDto> sendEmail(@Valid @RequestBody EmailRequestDto request) {
        enrichCorrelationId(request);
        log.info("POST /send [type={}] [to={}] [correlationId={}]",
                request.getEmailType(), maskEmail(request.getToEmail()), request.getCorrelationId());

        EmailResponseDto response = emailService.sendEmail(request);
        return ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<OtpResponseDto> sendOtp(@Valid @RequestBody OtpRequestDto request) {
        log.info("POST /send/otp [to={}]", maskEmail(request.getToEmail()));
        OtpResponseDto response = otpService.sendOtp(request);
        return ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<BulkEmailResponseDto> sendBulkEmail(@Valid @RequestBody BulkEmailRequestDto request) {
        log.info("POST /send/bulk [recipients={}] [type={}]",
                request.getRecipients().size(), request.getEmailType());
        BulkEmailResponseDto response = queueService.enqueueBulk(request);
        return ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<EmailResponseDto> scheduleEmail(@Valid @RequestBody ScheduleEmailRequestDto request) {
        log.info("POST /schedule [to={}] [at={}]",
                maskEmail(request.getEmailRequest().getToEmail()), request.getScheduledAt());
        EmailResponseDto response = schedulerService.scheduleEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<EmailResponseDto> getEmailStatus(@PathVariable String messageId) {
        log.debug("GET /status/{}", messageId);
        return ResponseEntity.ok(emailService.getEmailStatus(messageId));
    }

    @Override
    public ResponseEntity<EmailResponseDto> cancelScheduledEmail(@PathVariable String messageId) {
        log.info("DELETE /schedule/{}", messageId);
        return ResponseEntity.ok(schedulerService.cancelScheduledEmail(messageId));
    }

    @Override
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // ── Additional endpoints ─────────────────────────────────────────────────

    @GetMapping("/api/v1/mail/queues")
    public ResponseEntity<?> getQueueDepths() {
        return ResponseEntity.ok(queueService.getQueueDepths());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void enrichCorrelationId(EmailRequestDto request) {
        if (request.getCorrelationId() == null || request.getCorrelationId().isBlank()) {
            request.setCorrelationId(UUID.randomUUID().toString());
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
