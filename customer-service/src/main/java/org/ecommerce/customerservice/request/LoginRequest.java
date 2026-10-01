package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload.
 *
 * @param email customer email used as the login identifier
 * @param password raw password to verify
 */
public record LoginRequest(
        @Email(message = "Email should be valid") @NotBlank String email,
        @NotBlank String password
) {
}

