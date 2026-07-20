package org.ecommerce.customerservice.service.impl;

import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.mapper.CustomerMapper;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerRequest request;
    private Customer customer;
    private CustomerResponse response;

    @BeforeEach
    void setUp() {
        request = TestDataFactory.request();
        customer = TestDataFactory.customer();
        response = TestDataFactory.response();
    }

    @Test
    void createCustomer_shouldSaveMappedEntityAndReturnId() {
        when(customerMapper.toCustomer(request)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(customer);

        String result = customerService.createCustomer(request);

        assertThat(result).isEqualTo("cust-1");
        verify(customerMapper).toCustomer(request);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_shouldApplyPartialUpdateAndSaveExistingEntity() {
        when(customerRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        doNothing().when(customerMapper).partialUpdate(request, customer);

        customerService.updateCustomer("cust-1", request);

        verify(customerMapper).partialUpdate(request, customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_shouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer("missing", request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id: missing");

        verify(customerMapper, never()).partialUpdate(any(), any());
    }

    @Test
    void findAllCustomers_shouldMapAllCustomersInPage() {
        var pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toCustomerResponse(customer)).thenReturn(response);

        var result = customerService.findAllCustomers(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void existsById_shouldReturnRepositoryResult() {
        when(customerRepository.existsById("cust-1")).thenReturn(true);

        assertThat(customerService.existsById("cust-1")).isTrue();
    }

    @Test
    void findById_shouldReturnMappedResponse() {
        when(customerRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(customerMapper.toCustomerResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.findById("cust-1");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById("missing"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id: missing");
    }

    @Test
    void deleteCustomer_shouldDelegateToRepository() {
        customerService.deleteCustomer("cust-1");

        verify(customerRepository).deleteById("cust-1");
    }
}


