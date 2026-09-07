package org.ecommerce.customerservice.response;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Boolean isActive,
        Boolean isEmailVerified
) {
}
