package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Customer profile management contract.
 */
public interface CustomerService {

    /** @return identifier of the newly created customer */
    UUID createCustomer(final CustomerRequest request);

    /** Updates mutable profile fields for an existing customer. */
    void updateCustomer(final UUID customerId, final CustomerRequest request);

    /** @return paged view of active customers */
    Page<CustomerResponse> findAllCustomers(Pageable pageable);

    /** @return whether an active customer exists for the given id */
    Boolean existsById(UUID customerId);

    /** @return active customer details for the given id */
    CustomerResponse findById(UUID customerId);

    /** Soft-deletes the customer identified by the given id. */
    void deleteCustomer(UUID customerId);
}
