package org.ecommerce.customerservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.exception.DuplicateEmailException;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.mapper.CustomerMapper;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.ecommerce.customerservice.service.PasswordService;
import org.ecommerce.customerservice.service.RolePermissionService;
import org.ecommerce.customerservice.config.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.UUID;

/**
 * Default implementation of customer registration and profile management.
 *
 * <p>This service applies business rules around duplicate emails, password hashing,
 * default role assignment, soft deletion, and cache population/eviction for hot read paths.</p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final RolePermissionService rolePermissionService;
    private final PasswordService passwordService;
    private final CustomerMapper customerMapper;

    /**
     * Registers a new customer account and assigns the default user role.
     */
    @Transactional
    @Override
    public UUID createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException("Email already exists: " + request.email());
        }

        var customer = customerMapper.toCustomer(request);
        customer.setPasswordHash(passwordService.encrypt(request.password()));
        customer.getRoles().add(rolePermissionService.getDefaultUserRole());
        return customerRepository.save(customer).getId();
    }

    /**
     * Updates mutable customer fields and evicts any stale cache entries.
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CUSTOMERS_CACHE, key = "#customerId"),
            @CacheEvict(cacheNames = CacheConfig.CUSTOMER_EXISTS_CACHE, key = "#customerId")
    })
    public void updateCustomer(UUID customerId, CustomerRequest request) {
        var customer = customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));

        boolean emailChanged = !customer.getEmail().equalsIgnoreCase(request.email());
        if (emailChanged && customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException("Email already exists: " + request.email());
        }

        customerMapper.partialUpdate(request, customer);

        if (request.password() != null && !request.password().isBlank()) {
            customer.setPasswordHash(passwordService.encrypt(request.password()));
        }
        customerRepository.save(customer);

    }

    /**
     * Returns a page of active customers.
     */
    @Override
    public Page<CustomerResponse> findAllCustomers(Pageable pageable) {
        return customerRepository.findByDeletedAtIsNull(pageable).map(customerMapper::toCustomerResponse);
    }

    /**
     * Cached existence check used by downstream services and authorization flows.
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.CUSTOMER_EXISTS_CACHE, key = "#customerId")
    public Boolean existsById(UUID customerId) {
        return customerRepository.existsByIdAndDeletedAtIsNull(customerId);
    }

    /**
     * Cached because order-service calls this on every order creation to validate the buyer and
     * snapshot their email; customer records change far less often than orders are placed.
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.CUSTOMERS_CACHE, key = "#customerId")
    public CustomerResponse findById(UUID customerId) {
        return customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .map(customerMapper::toCustomerResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
    }

    /**
     * Soft-deletes a customer and clears any cached lookup results.
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CUSTOMERS_CACHE, key = "#customerId"),
            @CacheEvict(cacheNames = CacheConfig.CUSTOMER_EXISTS_CACHE, key = "#customerId")
    })
    public void deleteCustomer(UUID customerId) {
        var customer = customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
        customer.setDeletedAt(java.time.Instant.now());
        customerRepository.save(customer);
    }
}
