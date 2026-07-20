package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.ecommerce.customerservice.entity.Address;

public record CustomerRequest(
        @NotNull(message = "First name is required")
        String firstName,

        @NotNull(message = "Last name is required")
        String lastName,

        @NotNull(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        Address address
) {
}
