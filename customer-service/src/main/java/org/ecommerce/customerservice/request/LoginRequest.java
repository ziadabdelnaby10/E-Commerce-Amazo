package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email should be valid") @NotBlank String email,
        @NotBlank String password
) {
}

