package org.ecommerce.customerservice.response;

import java.util.UUID;

/**
 * Public-facing customer projection returned by read endpoints and cache entries.
 *
 * @param id customer identifier
 * @param firstName customer's given name
 * @param lastName customer's family name
 * @param email customer email
 * @param phoneNumber optional phone number
 * @param isActive whether the account is active
 * @param isEmailVerified whether the email address has been verified
 */
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
