package org.ecommerce.notificationservice.infrastructure.client;

public record CustomerResponse(
        String id,
        String firstName,
        String lastName,
        String email
) {
}

