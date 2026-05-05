package com.eduai.mailservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpGeneratorTest {

    private final OtpGenerator generator = new OtpGenerator();

    @Test
    void generateNumeric_shouldProduceCorrectLength() {
        String otp = generator.generateNumeric(6);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d+"), "OTP should be numeric");
    }

    @Test
    void generateAlphanumeric_shouldProduceCorrectLength() {
        String otp = generator.generateAlphanumeric(8);
        assertEquals(8, otp.length());
    }

    @Test
    void generate_defaultLength_is6() {
        String otp = generator.generate();
        assertEquals(6, otp.length());
    }

    @Test
    void generateNumeric_invalidLength_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> generator.generateNumeric(3));
        assertThrows(IllegalArgumentException.class, () -> generator.generateNumeric(11));
    }

    @Test
    void generate_uniqueEachTime() {
        String otp1 = generator.generate();
        String otp2 = generator.generate();
        // Extremely unlikely to collide with 6-digit random
        assertNotEquals(otp1, otp2);
    }
}
