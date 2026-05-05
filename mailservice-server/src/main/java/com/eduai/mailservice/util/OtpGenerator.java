package com.eduai.mailservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure OTP values.
 */
@Component
public class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String NUMERIC_CHARS  = "0123456789";
    private static final String ALPHA_CHARS    = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // avoids ambiguous chars

    /**
     * Generate a numeric OTP of specified length.
     *
     * @param length OTP digit count (4–10)
     * @return Numeric OTP string
     */
    public String generateNumeric(int length) {
        validateLength(length);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC_CHARS.charAt(SECURE_RANDOM.nextInt(NUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generate an alphanumeric OTP of specified length.
     * Excludes visually ambiguous characters (0, O, I, 1).
     *
     * @param length OTP character count (4–10)
     * @return Alphanumeric OTP string
     */
    public String generateAlphanumeric(int length) {
        validateLength(length);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_CHARS.charAt(SECURE_RANDOM.nextInt(ALPHA_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a numeric OTP with default length (6).
     */
    public String generate() {
        return generateNumeric(Constants.DEFAULT_OTP_LENGTH);
    }

    /**
     * Generate a numeric OTP of specified length.
     */
    public String generate(int length) {
        return generateNumeric(length);
    }

    private void validateLength(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("OTP length must be between 4 and 10, got: " + length);
        }
    }
}
