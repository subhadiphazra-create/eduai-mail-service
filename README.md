# EduAI Mail Service

Production-ready email microservice for the EduAI platform. Supports OTP, welcome, reminder,
promotional and transactional emails with async Redis queue, multiple provider fallback, scheduling,
and Thymeleaf HTML templates.

---

## Architecture

```
mailservice-client   ← shared contracts (DTOs, enums, MailApi, MailServiceClient)
mailservice-server   ← business logic, providers, queue, scheduler, templates
```

**Flow:**
```
HTTP Request → MailController → EmailService → Redis Queue → EmailQueueConsumer
                                                                    ↓
                                              TemplateService (Thymeleaf render)
                                                                    ↓
                                              Provider Chain: Resend → SendGrid → SMTP
                                                                    ↓
                                                            EmailLog (DB persist)
```

---

## Quick Start (Local Dev)

```bash
# 1. Clone and build
mvn clean install -DskipTests

# 2. Start all services (Redis + PostgreSQL + MailHog + app)
docker-compose up -d

# 3. Open MailHog to see sent emails
open http://localhost:8025

# 4. Test the API
curl -X GET http://localhost:8082/api/v1/mail/ping
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev` or `prod`) |
| `SMTP_ENABLED` | `true` | Enable SMTP provider |
| `SMTP_HOST` | `localhost` | SMTP host |
| `SMTP_PORT` | `1025` (dev) / `587` (prod) | SMTP port |
| `SMTP_USERNAME` | ─ | SMTP username |
| `SMTP_PASSWORD` | ─ | SMTP password |
| `SMTP_FROM_EMAIL` | `noreply@eduai.com` | Default sender |
| `RESEND_ENABLED` | `false` | Enable Resend provider |
| `RESEND_API_KEY` | ─ | Resend API key (`re_xxx`) |
| `RESEND_FROM_EMAIL` | ─ | Resend sender address |
| `SENDGRID_ENABLED` | `false` | Enable SendGrid provider |
| `SENDGRID_API_KEY` | ─ | SendGrid API key (`SG.xxx`) |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | ─ | Redis password |
| `DATABASE_URL` | ─ | PostgreSQL JDBC URL (prod) |
| `DATABASE_USERNAME` | ─ | Database user |
| `DATABASE_PASSWORD` | ─ | Database password |

---

## Switching to Upstash Redis

1. Create a Redis database at [upstash.com](https://upstash.com)
2. Set these environment variables:

```env
REDIS_HOST=<your-upstash-endpoint>.upstash.io
REDIS_PORT=6380
REDIS_PASSWORD=<your-upstash-password>
```

3. In `application-prod.yml`, `ssl.enabled` is already set to `true`.

---

## API Reference

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/mail/send` | Send single email |
| `POST` | `/api/v1/mail/send/otp` | Send OTP email |
| `POST` | `/api/v1/mail/send/bulk` | Send bulk email |
| `POST` | `/api/v1/mail/schedule` | Schedule email |
| `GET` | `/api/v1/mail/status/{id}` | Get email status |
| `DELETE` | `/api/v1/mail/schedule/{id}` | Cancel scheduled email |
| `GET` | `/api/v1/mail/queues` | Queue depth monitor |
| `GET` | `/api/v1/mail/ping` | Health check |

### Send OTP
```json
POST /api/v1/mail/send/otp
{
  "toEmail": "user@example.com",
  "toName": "John Doe",
  "purpose": "Login Verification",
  "expiryMinutes": 10,
  "appName": "MyApp",
  "supportEmail": "help@myapp.com"
}
```

### Send Welcome Email
```json
POST /api/v1/mail/send
{
  "toEmail": "user@example.com",
  "toName": "John",
  "subject": "Welcome to MyApp!",
  "emailType": "WELCOME",
  "templateVariables": {
    "dashboardUrl": "https://myapp.com/dashboard",
    "appName": "MyApp",
    "supportEmail": "help@myapp.com"
  }
}
```

### Send Bulk Email
```json
POST /api/v1/mail/send/bulk
{
  "subject": "Important Announcement",
  "emailType": "PROMOTIONAL",
  "globalTemplateVariables": { "appName": "MyApp" },
  "recipients": [
    { "email": "user1@example.com", "name": "User One" },
    { "email": "user2@example.com", "name": "User Two",
      "templateVariables": { "promoCode": "VIP50" } }
  ]
}
```

### Schedule Email
```json
POST /api/v1/mail/schedule
{
  "scheduledAt": "2025-12-01T09:00:00",
  "jobLabel": "Holiday Promo",
  "emailRequest": {
    "toEmail": "user@example.com",
    "subject": "Happy Holidays!",
    "emailType": "PROMOTIONAL",
    "templateVariables": { "promoCode": "HOLIDAY25" }
  }
}
```

---

## Using the Client SDK in Another App

### 1. Add dependency to consumer app's `pom.xml`

```xml
<dependency>
  <groupId>com.eduai</groupId>
  <artifactId>mailservice-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

### 2. Enable Feign in consumer app

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.eduai.mailservice.api")
public class MyApp { ... }
```

### 3. Configure URL in `application.yml`

```yaml
mail-service:
  url: http://eduai-mail-service:8082
```

### 4. Inject and call

```java
@Service
@RequiredArgsConstructor
public class MyUserService {

    private final MailServiceClient mailServiceClient;

    public void onUserRegistered(User user) {
        mailServiceClient.sendOtp(OtpRequestDto.builder()
            .toEmail(user.getEmail())
            .toName(user.getName())
            .purpose("Account Verification")
            .appName("MyApp")
            .correlationId(UUID.randomUUID().toString())
            .build());
    }
}
```

---

## Email Providers

| Provider | Priority | Use Case |
|---|---|---|
| Resend | 1 (highest) | Primary — best deliverability, great free tier |
| SendGrid | 2 | Secondary fallback |
| SMTP | 3 | Final fallback / local dev (MailHog) |

Enable multiple providers for automatic failover. The consumer will try providers in priority order.

---

## Templates Available

| Template Key | EmailType | Description |
|---|---|---|
| `otp` | OTP | OTP verification code |
| `welcome` | WELCOME | New user welcome |
| `reminder` | REMINDER | Deadline / action reminder |
| `promo` | PROMOTIONAL | Marketing / offer email |
| `notification` | NOTIFICATION | General notification |
| `update` | UPDATE | Account update alert |
| `password_reset` | PASSWORD_RESET | Password reset link |
| `transactional` | TRANSACTIONAL | Transaction confirmation |

All templates accept `toName`, `appName`, `supportEmail`, `year` as common variables.

---

## Queue Architecture

| Queue | Priority | Contents |
|---|---|---|
| `mail:queue:high` | Highest | OTP, password reset, transactional |
| `mail:queue:normal` | Medium | Welcome, reminder, notification, update |
| `mail:queue:low` | Lowest | Promotional, bulk |
| `mail:queue:retry` | ─ | Failed messages awaiting retry |
| `mail:queue:dlq` | ─ | Dead letter queue (exhausted retries) |

---

## Multi-App / Multi-Tenant

Pass `appId` in every request to isolate logs per application:

```json
{ "appId": "my-lms-app", "toEmail": "..." }
```

Filter logs per app via `GET /api/v1/mail/logs?appId=my-lms-app` (extend as needed).
