package org.ecommerce.notificationservice.application.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void renderReplacesPlaceholders() {
        String rendered = renderer.render(
                "Order {{orderNumber}} total {{amount}} {{currency}}",
                Map.of("orderNumber", "ORD-1", "amount", 120.5, "currency", "USD")
        );

        assertThat(rendered).isEqualTo("Order ORD-1 total 120.5 USD");
    }
}

