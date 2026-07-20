package org.ecommerce.customerservice.response;

public record CustomerResponse(
        String id,
        String firstName,
        String lastName,
        String email
) {
}
