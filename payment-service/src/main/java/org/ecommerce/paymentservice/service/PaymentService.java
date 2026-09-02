package org.ecommerce.paymentservice.service;

import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.InitiatePaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentSummaryResponse;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request);

    PaymentResponse getByPaymentId(String paymentId);

    Page<PaymentSummaryResponse> listByUser(String userId, PaymentStatus status, Pageable pageable);
}

