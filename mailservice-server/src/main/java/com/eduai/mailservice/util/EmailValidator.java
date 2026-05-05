package com.eduai.mailservice.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility for email address validation beyond basic Bean Validation.
 */
@Component
public class EmailValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern DISPOSABLE_DOMAIN_PATTERN = Pattern.compile(
            ".*(mailinator|guerrillamail|tempmail|throwam|yopmail|10minutemail|trashmail).*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validate basic email format.
     *
     * @param email email address to validate
     * @return true if valid format
     */
    public boolean isValid(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate and throw if invalid.
     *
     * @param email email address to validate
     * @throws IllegalArgumentException if invalid
     */
    public void validateOrThrow(String email) {
        if (!isValid(email)) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    /**
     * Check if the email belongs to a known disposable/temporary domain.
     *
     * @param email email address
     * @return true if disposable
     */
    public boolean isDisposable(String email) {
        if (email == null) return false;
        String domain = email.contains("@") ? email.split("@")[1] : email;
        return DISPOSABLE_DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * Extract domain from an email address.
     *
     * @param email email address
     * @return domain part
     */
    public String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        return email.split("@")[1].toLowerCase();
    }

    /**
     * Mask an email for safe logging/display.
     * Example: john.doe@example.com → j*******e@example.com
     */
    public String mask(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() <= 2
                ? "**"
                : local.charAt(0) + "*".repeat(Math.max(1, local.length() - 2)) + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
