package org.ecommerce.customerservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.mapper.CustomerMapper;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    private final CustomerMapper customerMapper;

    @Transactional
    @Override
    public String createCustomer(CustomerRequest request) {
        var customer = customerMapper.toCustomer(request);
        return customerRepository.save(customer).getId();
    }

    @Override
    public void updateCustomer(String customerId, CustomerRequest request) {
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
        customerMapper.partialUpdate(request, customer);
        customerRepository.save(customer);

    }

    @Override
    public Page<CustomerResponse> findAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toCustomerResponse);
    }

    @Override
    public Boolean existsById(String customerId) {
        return customerRepository.existsById(customerId);
    }

    @Override
    public CustomerResponse findById(String customerId) {
        return customerRepository.findById(customerId)
                .map(customerMapper::toCustomerResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
    }

    @Override
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }
}
