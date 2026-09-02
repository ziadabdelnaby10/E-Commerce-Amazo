package org.ecommerce.notificationservice.application.usecase.model;

public record SendEmailCommand(
        String to,
        String subject,
        String body,
        boolean html
) {
}

