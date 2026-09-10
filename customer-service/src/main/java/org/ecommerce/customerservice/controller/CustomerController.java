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

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<GeneralResponse<String>> createCustomer(@RequestBody @Valid CustomerRequest request) {
        var customerId = service.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneralResponse.of(HttpStatus.CREATED.value(), customerId.toString()));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAuthority('MODIFY_USER') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<Void>> updateCustomer(@PathVariable UUID customerId, @RequestBody @Valid CustomerRequest request) {
        service.updateCustomer(customerId, request);
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), null));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ResponseEntity<GeneralResponse<Page<CustomerResponse>>> findAll(
            @PageableDefault(size = 20, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.findAllCustomers(pageable)));
    }

    @GetMapping("/exists/{customerId}")
    @PreAuthorize("hasAnyAuthority('VIEW_USERS', 'ROLE_SYSTEM') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<Boolean>> existsById(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.existsById(customerId)));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('VIEW_USERS') or (authentication != null and authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) and authentication.principal.claims['userId'] == #customerId.toString())")
    public ResponseEntity<GeneralResponse<CustomerResponse>> findById(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), service.findById(customerId)));
    }

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
