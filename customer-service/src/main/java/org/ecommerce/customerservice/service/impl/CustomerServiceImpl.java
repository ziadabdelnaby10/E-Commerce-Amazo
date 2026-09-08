package org.ecommerce.customerservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.exception.DuplicateEmailException;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.repository.RoleRepository;
import org.ecommerce.customerservice.mapper.CustomerMapper;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.ecommerce.customerservice.service.PasswordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;//TODO Create a Role service for it
    private final PasswordService passwordService;
    private final CustomerMapper customerMapper;

    @Transactional
    @Override
    public UUID createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException("Email already exists: " + request.email());
        }

        var customer = customerMapper.toCustomer(request);
        customer.setPasswordHash(passwordService.encrypt(request.password()));
        roleRepository.findByName("ROLE_USER").ifPresent(role -> customer.getRoles().add(role));
        return customerRepository.save(customer).getId();
    }

    @Transactional
    @Override
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

    @Override
    public Page<CustomerResponse> findAllCustomers(Pageable pageable) {
        return customerRepository.findByDeletedAtIsNull(pageable).map(customerMapper::toCustomerResponse);
    }

    @Override
    public Boolean existsById(UUID customerId) {
        return customerRepository.existsByIdAndDeletedAtIsNull(customerId);
    }

    @Override
    public CustomerResponse findById(UUID customerId) {
        return customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .map(customerMapper::toCustomerResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
    }

    @Transactional
    @Override
    public void deleteCustomer(UUID customerId) {
        var customer = customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
        customer.setDeletedAt(java.time.Instant.now());
        customerRepository.save(customer);
    }
}
