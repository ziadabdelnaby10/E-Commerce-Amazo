package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload used for customer registration and profile updates.
 *
 * @param firstName customer's given name
 * @param lastName customer's family name
 * @param email unique customer email address
 * @param phoneNumber optional phone number
 * @param password raw password supplied during registration or password change
 */
public record CustomerRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        String phoneNumber,

        @NotBlank(message = "Password is required")
        String password
) {
}
