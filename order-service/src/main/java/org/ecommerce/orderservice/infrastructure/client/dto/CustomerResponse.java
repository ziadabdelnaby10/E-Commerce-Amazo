package org.ecommerce.orderservice.infrastructure.client.dto;

public record CustomerResponse(
        String id,
        String firstName,
        String lastName,
        String email
) {
}
