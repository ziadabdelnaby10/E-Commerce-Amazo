package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link org.ecommerce.customerservice.entity.Role}
 */
public record CreateRoleRequest(
        @NotNull(message = "Role name is required.")
        @NotBlank(message = "Role name cannot be blank.")
        String name,
        String description) implements Serializable {
}