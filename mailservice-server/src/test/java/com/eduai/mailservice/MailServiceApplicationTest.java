package com.eduai.mailservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic smoke test — verifies Spring context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("dev")
class MailServiceApplicationTest {

    @Test
    void contextLoads() {
        // If this passes, the Spring context assembled without errors.
    }
}
