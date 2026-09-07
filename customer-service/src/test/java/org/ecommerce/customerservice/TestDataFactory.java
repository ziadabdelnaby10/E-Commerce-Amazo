package org.ecommerce.customerservice;

import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;

import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static CustomerRequest request() {
        return new CustomerRequest("Ziad", "Hassan", "ziad@example.com", "+201000000000", "SecurePassword123");
    }

    public static CustomerRequest partialRequest() {
        return new CustomerRequest("Updated", "User", "updated@example.com", "+201111111111", "");
    }

    public static Customer customer() {
        return Customer.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .firstName("Ziad")
                .lastName("Hassan")
                .email("ziad@example.com")
                .phoneNumber("+201000000000")
                .isActive(true)
                .isEmailVerified(false)
                .passwordHash("$2a$10$encoded")
                .build();
    }

    public static CustomerResponse response() {
        return new CustomerResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Ziad",
                "Hassan",
                "ziad@example.com",
                "+201000000000",
                true,
                false
        );
    }
}

