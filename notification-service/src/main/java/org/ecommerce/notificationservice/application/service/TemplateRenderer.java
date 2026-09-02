package org.ecommerce.notificationservice.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateRenderer {

    public String render(String template, Map<String, Object> values) {
        if (template == null || template.isBlank() || values == null || values.isEmpty()) {
            return template;
        }

        String rendered = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }
}

