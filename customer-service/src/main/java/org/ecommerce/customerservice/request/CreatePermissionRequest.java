package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link org.ecommerce.customerservice.entity.Permission}
 */
public record CreatePermissionRequest(
        @NotNull(message = "Permission name is required.")
        @NotBlank(message = "Permission name cannot be blank.")
        String name,
        String description,
        @NotNull(message = "Permission category is required.")
        @NotBlank(message = "Permission category cannot be blank.")
        String category) implements Serializable {
}

