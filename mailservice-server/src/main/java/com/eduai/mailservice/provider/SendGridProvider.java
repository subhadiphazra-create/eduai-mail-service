package com.eduai.mailservice.provider;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.exception.EmailException;
import com.eduai.mailservice.util.Constants;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Email provider using the SendGrid API.
 * Suitable as a secondary/fallback provider for high-volume deployments.
 *
 * <p>Configure via:
 * <pre>
 * mail.providers.sendgrid.enabled=true
 * mail.providers.sendgrid.api-key=SG.xxxx
 * mail.providers.sendgrid.from-email=noreply@yourdomain.com
 * </pre>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "mail.providers.sendgrid.enabled", havingValue = "true")
public class SendGridProvider implements EmailProvider {

    private final SendGrid sendGrid;

    @Value("${mail.providers.sendgrid.from-email}")
    private String defaultFromEmail;

    @Value("${mail.providers.sendgrid.from-name:EduAI}")
    private String defaultFromName;

    @Value("${mail.providers.sendgrid.enabled:false}")
    private boolean enabled;

    public SendGridProvider(@Value("${mail.providers.sendgrid.api-key:}") String apiKey) {
        this.sendGrid = new SendGrid(apiKey.isBlank() ? "placeholder" : apiKey);
    }

    @Override
    public String send(EmailRequestDto request, String htmlBody) {
        try {
            String fromEmail = request.getFromEmail() != null ? request.getFromEmail() : defaultFromEmail;
            String fromName  = request.getFromName()  != null ? request.getFromName()  : defaultFromName;

            Email from = new Email(fromEmail, fromName);
            Email to   = new Email(request.getToEmail(),
                    request.getToName() != null ? request.getToName() : "");

            Content content = htmlBody != null && !htmlBody.isBlank()
                    ? new Content("text/html", htmlBody)
                    : new Content("text/plain",
                        request.getTextBody() != null ? request.getTextBody() : "");

            Mail mail = new Mail(from, request.getSubject(), to, content);

            if (request.getReplyTo() != null) {
                mail.setReplyTo(new Email(request.getReplyTo()));
            }

            com.sendgrid.helpers.mail.objects.TrackingSettings trackingSettings =
                    new com.sendgrid.helpers.mail.objects.TrackingSettings();

            OpenTrackingSetting openTracking = new OpenTrackingSetting();
            openTracking.setEnable(request.isTrackOpens());
            trackingSettings.setOpenTrackingSetting(openTracking);

            ClickTrackingSetting clickTracking =
                    new ClickTrackingSetting();
            clickTracking.setEnable(request.isTrackClicks());
            clickTracking.setEnableText(false);
            trackingSettings.setClickTrackingSetting(clickTracking);

            mail.setTrackingSettings(trackingSettings);

            if (request.getCc() != null) {
                Personalization personalization = mail.getPersonalization().get(0);
                request.getCc().forEach(cc -> personalization.addCc(new Email(cc)));
            }

            Request sgRequest = new Request();
            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");
            sgRequest.setBody(mail.build());

            Response response = sendGrid.api(sgRequest);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // SendGrid returns message ID in X-Message-Id header
                String msgId = response.getHeaders().getOrDefault("X-Message-Id", "sg-unknown");
                log.debug("SendGrid sent to [{}] msgId={}", request.getToEmail(), msgId);
                return msgId;
            } else {
                throw new RuntimeException("SendGrid returned HTTP " + response.getStatusCode()
                        + ": " + response.getBody());
            }
        } catch (EmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw EmailException.providerError(getProviderName(),
                    "SendGrid API error: " + ex.getMessage(), ex);
        }
    }

    @Override public String  getProviderName() { return Constants.PROVIDER_SENDGRID; }
    @Override public int     getPriority()      { return 2; }
    @Override public boolean isAvailable()      { return enabled; }
}
