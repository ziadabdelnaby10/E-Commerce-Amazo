package org.ecommerce.customerservice.service;

import jakarta.validation.Valid;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    String createCustomer(final CustomerRequest request);

    void updateCustomer(final String customerId, final CustomerRequest request);

    Page<CustomerResponse> findAllCustomers(Pageable pageable);

    Boolean existsById(String customerId);

    CustomerResponse findById(String customerId);

    void deleteCustomer(String customerId);
}
