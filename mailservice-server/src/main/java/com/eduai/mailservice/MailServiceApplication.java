package com.eduai.mailservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EduAI Mail Microservice
 *
 * <p>Production-ready email delivery service supporting:
 * <ul>
 *   <li>OTP, Welcome, Reminder, Promotional and Transactional emails</li>
 *   <li>Multiple providers: SMTP, Resend, SendGrid (with fallback)</li>
 *   <li>Redis-backed async queue with retry mechanism</li>
 *   <li>Scheduled / recurring email delivery</li>
 *   <li>Thymeleaf HTML template rendering</li>
 *   <li>Multi-tenant / multi-app support</li>
 * </ul>
 *
 * <p>Port: 8082
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableFeignClients
@ConfigurationPropertiesScan
public class MailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailServiceApplication.class, args);
    }
}
