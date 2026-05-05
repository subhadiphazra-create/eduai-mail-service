package com.eduai.mailservice.provider;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.exception.EmailException;
import com.eduai.mailservice.util.Constants;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SMTP email provider using Spring JavaMailSender.
 * Works with any SMTP server (Gmail, Outlook, custom relay, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mail.providers.smtp.enabled", havingValue = "true", matchIfMissing = true)
public class SmtpProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${mail.providers.smtp.from-email}")
    private String defaultFromEmail;

    @Value("${mail.providers.smtp.from-name:EduAI}")
    private String defaultFromName;

    @Value("${mail.providers.smtp.enabled:true}")
    private boolean enabled;

    @Override
    public String send(EmailRequestDto request, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, Constants.DEFAULT_CHARSET);

            String fromEmail = request.getFromEmail() != null ? request.getFromEmail() : defaultFromEmail;
            String fromName  = request.getFromName()  != null ? request.getFromName()  : defaultFromName;

            helper.setFrom(fromEmail, fromName);
            helper.setTo(request.getToEmail());
            helper.setSubject(request.getSubject());

            if (htmlBody != null && !htmlBody.isBlank()) {
                helper.setText(request.getTextBody() != null ? request.getTextBody() : "", htmlBody);
            } else if (request.getTextBody() != null) {
                helper.setText(request.getTextBody(), false);
            }

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }
            if (request.getReplyTo() != null) {
                helper.setReplyTo(request.getReplyTo());
            }

            mailSender.send(mimeMessage);

            String internalId = UUID.randomUUID().toString();
            log.debug("SMTP sent to [{}] internalId={}", request.getToEmail(), internalId);
            return internalId;

        } catch (Exception ex) {
            throw EmailException.providerError(getProviderName(),
                    "SMTP send failed: " + ex.getMessage(), ex);
        }
    }

    @Override public String  getProviderName() { return Constants.PROVIDER_SMTP; }
    @Override public int     getPriority()      { return 3; }  // fallback
    @Override public boolean isAvailable()      { return enabled; }
}
