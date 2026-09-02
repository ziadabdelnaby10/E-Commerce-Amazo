package org.ecommerce.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.InitiatePaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentSummaryResponse;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;
import org.ecommerce.paymentservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<InitiatePaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getByPaymentId(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getByPaymentId(paymentId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentSummaryResponse>> listByUser(
            @RequestParam String userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentSummaryResponse> response = paymentService.listByUser(userId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
}

