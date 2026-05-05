package com.eduai.mailservice.template;

import com.eduai.mailservice.exception.EmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * Renders Thymeleaf HTML email templates.
 * Templates live in src/main/resources/templates/.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateRenderer {

    private final TemplateEngine templateEngine;

    /**
     * Render a named Thymeleaf template with provided variables.
     *
     * @param templateName template file name (without .html extension)
     * @param variables    template variable map
     * @return rendered HTML string
     * @throws EmailException if rendering fails
     */
    public String render(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context(Locale.ENGLISH);
            if (variables != null) {
                context.setVariables(variables);
            }
            String html = templateEngine.process(templateName, context);
            log.debug("Rendered template [{}] ({} chars)", templateName, html.length());
            return html;
        } catch (Exception ex) {
            log.error("Template rendering failed for [{}]: {}", templateName, ex.getMessage(), ex);
            throw EmailException.templateError(templateName, ex);
        }
    }

    /**
     * Render with locale support.
     */
    public String render(String templateName, Map<String, Object> variables, Locale locale) {
        try {
            Context context = new Context(locale != null ? locale : Locale.ENGLISH);
            if (variables != null) {
                context.setVariables(variables);
            }
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            throw EmailException.templateError(templateName, ex);
        }
    }
}
