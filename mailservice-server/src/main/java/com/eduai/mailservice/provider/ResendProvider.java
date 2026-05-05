package com.eduai.mailservice.provider;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.exception.EmailException;
import com.eduai.mailservice.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Email provider using the Resend API (https://resend.com).
 * Resend is developer-friendly, has excellent deliverability and a generous free tier.
 *
 * <p>Configure via:
 * <pre>
 * mail.providers.resend.enabled=true
 * mail.providers.resend.api-key=re_xxxx
 * mail.providers.resend.from-email=noreply@yourdomain.com
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mail.providers.resend.enabled", havingValue = "true")
public class ResendProvider implements EmailProvider {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mail.providers.resend.api-key}")
    private String apiKey;

    @Value("${mail.providers.resend.from-email}")
    private String defaultFromEmail;

    @Value("${mail.providers.resend.from-name:EduAI}")
    private String defaultFromName;

    @Value("${mail.providers.resend.enabled:false}")
    private boolean enabled;

    @Override
    public String send(EmailRequestDto request, String htmlBody) {
        try {
            Map<String, Object> payload = new HashMap<>();

            String fromEmail = request.getFromEmail() != null ? request.getFromEmail() : defaultFromEmail;
            String fromName  = request.getFromName()  != null ? request.getFromName()  : defaultFromName;
            payload.put("from",    fromName + " <" + fromEmail + ">");
            payload.put("to",      new String[]{request.getToEmail()});
            payload.put("subject", request.getSubject());

            if (htmlBody != null && !htmlBody.isBlank()) {
                payload.put("html", htmlBody);
            }
            if (request.getTextBody() != null) {
                payload.put("text", request.getTextBody());
            }
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                payload.put("cc", request.getCc());
            }
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                payload.put("bcc", request.getBcc());
            }
            if (request.getReplyTo() != null) {
                payload.put("reply_to", request.getReplyTo());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    RESEND_API_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String id = (String) response.getBody().get("id");
                log.debug("Resend sent to [{}] id={}", request.getToEmail(), id);
                return id;
            } else {
                throw new RuntimeException("Non-2xx response: " + response.getStatusCode());
            }

        } catch (EmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw EmailException.providerError(getProviderName(),
                    "Resend API error: " + ex.getMessage(), ex);
        }
    }

    @Override public String  getProviderName() { return Constants.PROVIDER_RESEND; }
    @Override public int     getPriority()      { return 1; }  // preferred
    @Override public boolean isAvailable()      { return enabled && apiKey != null && !apiKey.isBlank(); }
}
