package com.eduai.mailservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void isValid_validEmails() {
        assertTrue(validator.isValid("user@example.com"));
        assertTrue(validator.isValid("user.name+tag@subdomain.example.co.uk"));
        assertTrue(validator.isValid("USER@EXAMPLE.COM"));
    }

    @Test
    void isValid_invalidEmails() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid("notanemail"));
        assertFalse(validator.isValid("missing@"));
        assertFalse(validator.isValid("@nodomain.com"));
    }

    @Test
    void mask_producesExpectedFormat() {
        String masked = validator.mask("john.doe@example.com");
        assertTrue(masked.contains("@example.com"));
        assertTrue(masked.startsWith("j"));
        assertTrue(masked.contains("*"));
    }

    @Test
    void isDisposable_detectsKnownDisposable() {
        assertTrue(validator.isDisposable("user@mailinator.com"));
        assertFalse(validator.isDisposable("user@gmail.com"));
    }

    @Test
    void validateOrThrow_throwsForInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateOrThrow("invalid-email"));
    }
}
