package org.ecommerce.customerservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> createCustomer(@RequestBody @Valid CustomerRequest request) {
        var customerId = service.createCustomer(request);
        return new ResponseEntity<>(customerId, HttpStatus.CREATED);
    }

    @PutMapping("/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> updateCustomer(@PathVariable String customerId, @RequestBody @Valid CustomerRequest request) {
        service.updateCustomer(customerId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<CustomerResponse>> findAll(
            @PageableDefault(size = 20, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(service.findAllCustomers(pageable));
    }

    @GetMapping("/exists/{customerId}")
    public ResponseEntity<Boolean> existsById(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(service.existsById(customerId));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> findById(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(service.findById(customerId));
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @PathVariable String customerId
    ) {
        this.service.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
