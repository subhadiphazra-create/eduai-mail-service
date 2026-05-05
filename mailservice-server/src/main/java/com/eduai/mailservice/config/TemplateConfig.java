package com.eduai.mailservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Thymeleaf template engine configuration for HTML email rendering.
 */
@Configuration
public class TemplateConfig {

    @Bean
    public ITemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);                  // cache in production
        resolver.setCacheTTLMs(3_600_000L);           // 1 hour TTL
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
