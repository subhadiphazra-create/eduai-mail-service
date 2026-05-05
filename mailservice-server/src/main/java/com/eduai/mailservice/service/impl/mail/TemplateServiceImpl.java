package com.eduai.mailservice.service.impl.mail;

import com.eduai.mailservice.service.mail.TemplateService;
import com.eduai.mailservice.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Template service implementation wrapping TemplateRenderer with caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRenderer templateRenderer;

    @Override
    public String render(String templateName, Map<String, Object> variables) {
        log.debug("Rendering template [{}]", templateName);
        return templateRenderer.render(templateName, variables);
    }
}
