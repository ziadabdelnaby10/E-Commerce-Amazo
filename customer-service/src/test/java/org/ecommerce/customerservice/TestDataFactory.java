package org.ecommerce.customerservice;

import org.ecommerce.customerservice.entity.Address;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Address address() {
        return Address.builder()
                .street("Main Street")
                .houseNumber("10A")
                .zipCode("12345")
                .build();
    }

    public static CustomerRequest request() {
        return new CustomerRequest("Ziad", "Hassan", "ziad@example.com", address());
    }

    public static CustomerRequest partialRequest() {
        return new CustomerRequest("Updated", null, null, null);
    }

    public static Customer customer() {
        return Customer.builder()
                .id("cust-1")
                .firstName("Ziad")
                .lastName("Hassan")
                .email("ziad@example.com")
                .address(address())
                .build();
    }

    public static CustomerResponse response() {
        return new CustomerResponse("cust-1", "Ziad", "Hassan", "ziad@example.com");
    }
}

