package org.ecommerce.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.GeneralResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCESS_PAYMENT')")
    public ResponseEntity<GeneralResponse<InitiatePaymentResponse>> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneralResponse.of(HttpStatus.CREATED.value(), paymentService.initiatePayment(request)));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<GeneralResponse<PaymentResponse>> getByPaymentId(@PathVariable String paymentId) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), paymentService.getByPaymentId(paymentId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS') and (hasAuthority('VIEW_USERS') "
            + "or (authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) "
            + "and authentication.principal.claims['userId'] == #userId))")
    public ResponseEntity<GeneralResponse<Page<PaymentSummaryResponse>>> listByUser(
            @RequestParam String userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentSummaryResponse> response = paymentService.listByUser(userId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), response));
    }
}

