package org.ecommerce.customerservice.mapper;

import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void toCustomer_shouldMapAllFields() {
        CustomerRequest request = TestDataFactory.request();

        Customer customer = customerMapper.toCustomer(request);

        assertThat(customer.getFirstName()).isEqualTo(request.firstName());
        assertThat(customer.getLastName()).isEqualTo(request.lastName());
        assertThat(customer.getEmail()).isEqualTo(request.email());
        assertThat(customer.getPhoneNumber()).isEqualTo(request.phoneNumber());
    }

    @Test
    void toCustomerResponse_shouldMapExpectedFields() {
        Customer customer = TestDataFactory.customer();

        var response = customerMapper.toCustomerResponse(customer);

        assertThat(response).usingRecursiveComparison().isEqualTo(TestDataFactory.response());
    }

    @Test
    void partialUpdate_shouldIgnoreNullValues() {
        Customer customer = TestDataFactory.customer();
        CustomerRequest partialRequest = TestDataFactory.partialRequest();

        customerMapper.partialUpdate(partialRequest, customer);

        assertThat(customer.getFirstName()).isEqualTo("Updated");
        assertThat(customer.getLastName()).isEqualTo("User");
        assertThat(customer.getEmail()).isEqualTo("updated@example.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("+201111111111");
    }
}

