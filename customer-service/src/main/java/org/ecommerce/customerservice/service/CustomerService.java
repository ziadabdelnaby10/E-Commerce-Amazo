package org.ecommerce.customerservice.service;

import jakarta.validation.Valid;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {
    UUID createCustomer(final CustomerRequest request);

    void updateCustomer(final UUID customerId, final CustomerRequest request);

    Page<CustomerResponse> findAllCustomers(Pageable pageable);

    Boolean existsById(UUID customerId);

    CustomerResponse findById(UUID customerId);

    void deleteCustomer(UUID customerId);
}
