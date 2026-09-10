package org.ecommerce.customerservice.service.impl;

import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.exception.DefaultRoleMissingException;
import org.ecommerce.customerservice.exception.DuplicateEmailException;
import org.ecommerce.customerservice.mapper.CustomerMapper;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.PasswordService;
import org.ecommerce.customerservice.service.RolePermissionService;
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
import java.util.UUID;

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

    @Mock
    private RolePermissionService rolePermissionService;

    @Mock
    private PasswordService passwordService;

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
        when(customerRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(customerMapper.toCustomer(request)).thenReturn(customer);
        when(passwordService.encrypt(request.password())).thenReturn("$2a$10$encoded");
        when(rolePermissionService.getDefaultUserRole()).thenReturn(Role.builder().id(1L).name("ROLE_USER").build());
        when(customerRepository.save(customer)).thenReturn(customer);

        UUID result = customerService.createCustomer(request);

        assertThat(result).isEqualTo(customer.getId());
        assertThat(customer.getPasswordHash()).isEqualTo("$2a$10$encoded");
        verify(customerMapper).toCustomer(request);
        verify(customerRepository).save(customer);
    }

    @Test
    void createCustomer_shouldFailWhenDefaultRoleIsMissing() {
        when(customerRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(customerMapper.toCustomer(request)).thenReturn(customer);
        when(passwordService.encrypt(request.password())).thenReturn("$2a$10$encoded");
        when(rolePermissionService.getDefaultUserRole()).thenThrow(new DefaultRoleMissingException("Default role is missing: ROLE_USER"));

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DefaultRoleMissingException.class)
                .hasMessage("Default role is missing: ROLE_USER");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomer_shouldThrowWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Email already exists: " + request.email());

        verify(customerMapper, never()).toCustomer(any());
    }

    @Test
    void updateCustomer_shouldApplyPartialUpdateAndSaveExistingEntity() {
        UUID customerId = customer.getId();
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        doNothing().when(customerMapper).partialUpdate(request, customer);
        when(passwordService.encrypt(request.password())).thenReturn("$2a$10$new");

        customerService.updateCustomer(customerId, request);

        verify(customerMapper).partialUpdate(request, customer);
        assertThat(customer.getPasswordHash()).isEqualTo("$2a$10$new");
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomer_shouldThrowWhenCustomerDoesNotExist() {
        UUID missingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(customerRepository.findByIdAndDeletedAtIsNull(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(missingId, request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id: " + missingId);

        verify(customerMapper, never()).partialUpdate(any(), any());
    }

    @Test
    void findAllCustomers_shouldMapAllCustomersInPage() {
        var pageable = PageRequest.of(0, 10);
        when(customerRepository.findByDeletedAtIsNull(pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toCustomerResponse(customer)).thenReturn(response);

        var result = customerService.findAllCustomers(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void existsById_shouldReturnRepositoryResult() {
        UUID customerId = customer.getId();
        when(customerRepository.existsByIdAndDeletedAtIsNull(customerId)).thenReturn(true);

        assertThat(customerService.existsById(customerId)).isTrue();
    }

    @Test
    void findById_shouldReturnMappedResponse() {
        UUID customerId = customer.getId();
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toCustomerResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.findById(customerId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrowWhenCustomerDoesNotExist() {
        UUID missingId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(customerRepository.findByIdAndDeletedAtIsNull(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(missingId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id: " + missingId);
    }

    @Test
    void deleteCustomer_shouldMarkDeletedAt() {
        UUID customerId = customer.getId();
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customerId);

        assertThat(customer.getDeletedAt()).isNotNull();
        verify(customerRepository).save(customer);
    }
}


