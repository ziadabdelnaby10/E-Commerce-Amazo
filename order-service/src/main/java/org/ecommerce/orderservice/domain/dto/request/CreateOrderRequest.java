package org.ecommerce.orderservice.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.ecommerce.orderservice.domain.dto.AddressDto;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public record CreateOrderRequest(
        @NotNull AddressDto shippingAddress,
        AddressDto billingAddress,
        String notes,
        @NotEmpty List<CreateOrderItemRequest> items
) {
}

