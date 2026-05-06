package com.eduai.mailservice.service.impl.mail;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.request.OtpRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;
import com.eduai.mailservice.dto.response.OtpResponseDto;
import com.eduai.mailservice.enums.mail.EmailType;
import com.eduai.mailservice.exception.OtpException;
import com.eduai.mailservice.repository.mail.EmailLogRepository;
import com.eduai.mailservice.service.mail.EmailService;
import com.eduai.mailservice.service.mail.OtpService;
import com.eduai.mailservice.util.Constants;
import com.eduai.mailservice.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OTP email service implementation.
 * Handles OTP generation, rate limiting via Redis, and email dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpGenerator              otpGenerator;
    private final EmailService              emailService;
    private final EmailLogRepository        emailLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OtpResponseDto sendOtp(OtpRequestDto request) {
        String email = request.getToEmail();

        // Rate limit: max N OTP per hour per email
        enforceRateLimit(email);

        // Generate OTP if not provided
        String otp = (request.getOtp() != null && !request.getOtp().isBlank())
                ? request.getOtp()
                : otpGenerator.generate(request.getOtpLength());

        // Store OTP in Redis for verification
        String otpKey = Constants.CACHE_OTP_PREFIX + email;

        // Delete any existing OTP for this email first
        redisTemplate.delete(otpKey);

        // Store new OTP
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(request.getExpiryMinutes()));

        // Build email request
        Map<String, Object> vars = buildTemplateVars(request, otp);
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId() : UUID.randomUUID().toString();

        EmailRequestDto emailRequest = EmailRequestDto.builder()
                .toEmail(email)
                .toName(request.getToName())
                .subject("Your OTP" + (request.getPurpose() != null ? " for " + request.getPurpose() : ""))
                .emailType(EmailType.OTP)
                .templateVariables(vars)
                .retryEnabled(false)
                .maxRetries(1)
                .correlationId(correlationId)
                .appId(request.getAppId())
                .preferredProvider(request.getPreferredProvider())
                .build();

        log.info("Sending OTP to [{}] purpose=[{}] expiry={}min correlationId={}",
                maskEmail(email), request.getPurpose(), request.getExpiryMinutes(), correlationId);

        try {
            EmailResponseDto response = emailService.sendEmail(emailRequest);
            return OtpResponseDto.success(response.getMessageId(), email,
                    request.getExpiryMinutes(), correlationId);
        } catch (Exception ex) {
            // Clean up Redis key if email sending fails
            redisTemplate.delete(otpKey);
            throw OtpException.sendFailed(email, ex);
        }
    }
    // ── Private helpers ──────────────────────────────────────────────────────

    private void enforceRateLimit(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count = emailLogRepository.countOtpSentToEmail(email, oneHourAgo);
        if (count >= Constants.OTP_RATE_LIMIT_PER_HOUR) {
            throw OtpException.rateLimitExceeded(email);
        }
    }

    private Map<String, Object> buildTemplateVars(OtpRequestDto request, String otp) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("otp",           otp);
        vars.put("toName",        request.getToName() != null ? request.getToName() : "User");
        vars.put("purpose",       request.getPurpose());
        vars.put("expiryMinutes", request.getExpiryMinutes());
        vars.put("appName",       request.getAppName() != null ? request.getAppName() : "EduAI");
        vars.put("appLogoUrl",    request.getAppLogoUrl());
        vars.put("supportEmail",  request.getSupportEmail());
        vars.put("year",          LocalDateTime.now().getYear());

        if (request.getAdditionalVariables() != null) {
            vars.putAll(request.getAdditionalVariables());
        }
        return vars;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
