package com.eduai.mailservice.util;

/**
 * Application-wide constants for keys, headers, and configuration defaults.
 */
public final class Constants {

    private Constants() {}

    // ── Redis Queue Keys ────────────────────────────────────────────────────
    public static final String QUEUE_EMAIL_HIGH    = "mail:queue:high";
    public static final String QUEUE_EMAIL_NORMAL  = "mail:queue:normal";
    public static final String QUEUE_EMAIL_LOW     = "mail:queue:low";
    public static final String QUEUE_EMAIL_DLQ     = "mail:queue:dlq";
    public static final String QUEUE_EMAIL_RETRY   = "mail:queue:retry";

    // ── Redis Cache Keys ────────────────────────────────────────────────────
    public static final String CACHE_OTP_PREFIX    = "mail:otp:";
    public static final String CACHE_RATE_PREFIX   = "mail:rate:";
    public static final String CACHE_TEMPLATE      = "mail:template:";

    // ── HTTP Headers ────────────────────────────────────────────────────────
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_APP_ID         = "X-App-ID";
    public static final String HEADER_REQUEST_ID     = "X-Request-ID";

    // ── Provider Names ──────────────────────────────────────────────────────
    public static final String PROVIDER_SMTP     = "smtp";
    public static final String PROVIDER_RESEND   = "resend";
    public static final String PROVIDER_SENDGRID = "sendgrid";

    // ── Defaults ────────────────────────────────────────────────────────────
    public static final int    DEFAULT_OTP_LENGTH        = 6;
    public static final int    DEFAULT_OTP_EXPIRY_MINS   = 10;
    public static final int    DEFAULT_MAX_RETRIES        = 3;
    public static final long   DEFAULT_RETRY_DELAY_MS    = 5_000L;
    public static final int    DEFAULT_BULK_RATE_LIMIT   = 10;
    public static final int    OTP_RATE_LIMIT_PER_HOUR   = 5;
    public static final int    MAX_BULK_RECIPIENTS        = 1000;

    // ── Scheduler ──────────────────────────────────────────────────────────
    public static final String SCHEDULER_FIXED_RATE  = "60000";    // 1 min
    public static final String RETRY_FIXED_RATE      = "30000";    // 30 sec

    // ── Template Names ──────────────────────────────────────────────────────
    public static final String TEMPLATE_OTP          = "otp";
    public static final String TEMPLATE_WELCOME      = "welcome";
    public static final String TEMPLATE_REMINDER     = "reminder";
    public static final String TEMPLATE_PROMO        = "promo";
    public static final String TEMPLATE_NOTIFICATION = "notification";
    public static final String TEMPLATE_UPDATE       = "update";
    public static final String TEMPLATE_PASSWORD_RESET = "password_reset";

    // ── Misc ────────────────────────────────────────────────────────────────
    public static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    public static final String DEFAULT_CHARSET   = "UTF-8";
}
