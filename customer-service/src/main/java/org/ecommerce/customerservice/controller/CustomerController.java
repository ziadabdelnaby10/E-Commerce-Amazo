package org.ecommerce.customerservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.response.GeneralResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for customer registration and profile management.
 *
 * <p>Read and write operations are protected with method-level authorization rules so
 * administrators can manage any customer while regular users can only act on their own
 * profile as identified by the JWT's {@code userId} claim.</p>
 */
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    /**
     * Registers a new customer account.
     *
     * @param request incoming customer registration payload
     * @return created customer id inside the standard response envelope
     */
    @PostMapping
    public ResponseEntity<GeneralResponse<String>> createCustomer(@RequestBody @Valid CustomerRequest request) {
        var customerId = service.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneralResponse.of(HttpStatus.CREATED.value(), customerId.toString()));
    }

    /**
     * Updates an existing customer profile.
     *
     * @param customerId customer identifier taken from the URL path
     * @param request mutable customer fields to update
     * @return empty success response
     */
    @PutMapping("/{customerId}")
    @PreAuthorize("hasAuthority('MODIFY_USER') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<Void>> updateCustomer(@PathVariable UUID customerId, @RequestBody @Valid CustomerRequest request) {
        service.updateCustomer(customerId, request);
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), null));
    }

    /**
     * Lists non-deleted customers with pagination support.
     *
     * @param pageable paging and sorting parameters
     * @return paged list of customer projections
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ResponseEntity<GeneralResponse<Page<CustomerResponse>>> findAll(
            @PageableDefault(size = 20, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.findAllCustomers(pageable)));
    }

    /**
     * Checks whether an active customer exists.
     *
     * @param customerId customer identifier to test
     * @return boolean existence flag
     */
    @GetMapping("/exists/{customerId}")
    @PreAuthorize("hasAnyAuthority('VIEW_USERS', 'ROLE_SYSTEM') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<Boolean>> existsById(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.existsById(customerId)));
    }

    /**
     * Retrieves a single active customer record.
     *
     * @param customerId customer identifier
     * @return customer response DTO
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('VIEW_USERS') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<CustomerResponse>> findById(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.findById(customerId)));
    }

    /**
     * Soft-deletes a customer account.
     *
     * @param customerId customer identifier
     * @return no-content response envelope
     */
    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasAuthority('DELETE_USER') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<Void>> delete(
            @PathVariable UUID customerId
    ) {
        this.service.deleteCustomer(customerId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(GeneralResponse.of(HttpStatus.NO_CONTENT.value(), null));
    }
}
