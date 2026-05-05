package com.eduai.mailservice.service.mail;

import java.util.Map;

/**
 * Template rendering service contract.
 */
public interface TemplateService {

    /**
     * Render a named Thymeleaf template with provided variables.
     *
     * @param templateName name of the template (without .html)
     * @param variables    template context variables
     * @return rendered HTML string
     */
    String render(String templateName, Map<String, Object> variables);
}
