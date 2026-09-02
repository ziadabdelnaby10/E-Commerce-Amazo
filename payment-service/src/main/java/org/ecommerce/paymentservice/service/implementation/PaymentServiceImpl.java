package org.ecommerce.paymentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.InitiatePaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentSummaryResponse;
import org.ecommerce.paymentservice.domain.model.Payment;
import org.ecommerce.paymentservice.domain.model.PaymentAuditLog;
import org.ecommerce.paymentservice.domain.model.PaymentMethodType;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;
import org.ecommerce.paymentservice.domain.model.PaymentTransaction;
import org.ecommerce.paymentservice.exception.PaymentNotFoundException;
import org.ecommerce.paymentservice.infrastructure.mapping.PaymentMapper;
import org.ecommerce.paymentservice.infrastructure.messaging.PaymentEventPublisher;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentAuditLogJpaRepository;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentJpaRepository;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import org.ecommerce.paymentservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal AUTO_APPROVAL_LIMIT = BigDecimal.valueOf(10_000);

    private final PaymentJpaRepository paymentRepository;
    private final PaymentTransactionJpaRepository paymentTransactionRepository;
    private final PaymentAuditLogJpaRepository paymentAuditLogRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        Payment existing = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(request.orderId()).orElse(null);
        if (existing != null && isActive(existing.getStatus())) {
            return paymentMapper.toInitiateResponse(existing);
        }

        Payment payment = paymentMapper.toPayment(request);
        payment.setPaymentId(generatePaymentId());
        payment.setCurrency(request.currency());
        payment.setPaymentMethod(PaymentMethodType.CREDIT_CARD);
        payment.setStatus(evaluateStatus(request.amount()));
        payment.setMerchantReference("ORDER-" + request.orderId());
        payment.setMetadata(objectMapper.valueToTree(Map.of("orderId", request.orderId())));
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            payment.setAuthorizedAt(Instant.now());
            payment.setCapturedAt(Instant.now());
            payment.setStatus(PaymentStatus.CAPTURED);
        } else {
            payment.setFailedAt(Instant.now());
        }

        Payment saved = paymentRepository.save(payment);

        paymentTransactionRepository.save(buildTransaction(saved));
        paymentAuditLogRepository.save(buildAuditLog(saved));

        publishPaymentEvent(saved);

        if (saved.getStatus() == PaymentStatus.CAPTURED) {
            return new InitiatePaymentResponse(saved.getPaymentId(), PaymentStatus.AUTHORIZED.name(), null);
        }
        return new InitiatePaymentResponse(saved.getPaymentId(), saved.getStatus().name(), "Payment authorization failed");
    }

    @Override
    public PaymentResponse getByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findByPaymentIdWithTransactions(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public Page<PaymentSummaryResponse> listByUser(String userId, PaymentStatus status, Pageable pageable) {
        return (status == null
                ? paymentRepository.findSummariesByUser(userId, pageable)
                : paymentRepository.findSummariesByUserAndStatus(userId, status, pageable))
                .map(paymentMapper::toSummary);
    }

    private PaymentStatus evaluateStatus(BigDecimal amount) {
        return amount.compareTo(AUTO_APPROVAL_LIMIT) <= 0 ? PaymentStatus.AUTHORIZED : PaymentStatus.FAILED;
    }

    private boolean isActive(PaymentStatus status) {
        return status == PaymentStatus.PENDING || status == PaymentStatus.AUTHORIZED || status == PaymentStatus.CAPTURED;
    }

    private String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentTransaction buildTransaction(Payment payment) {
        return PaymentTransaction.builder()
                .payment(payment)
                .transactionType("AUTHORIZATION")
                .amount(payment.getAmount())
                .gatewayTransactionId("GTW-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .status(payment.getStatus())
                .responseCode(payment.getStatus() == PaymentStatus.CAPTURED ? "00" : "05")
                .responseMessage(payment.getStatus() == PaymentStatus.CAPTURED ? "APPROVED" : "DECLINED")
                .createdAt(Instant.now())
                .build();
    }

    private PaymentAuditLog buildAuditLog(Payment payment) {
        return PaymentAuditLog.builder()
                .payment(payment)
                .action(payment.getStatus() == PaymentStatus.CAPTURED ? "CAPTURED" : "FAILED")
                .actor("SYSTEM")
                .oldStatus(PaymentStatus.PENDING)
                .newStatus(payment.getStatus())
                .reason(payment.getStatus() == PaymentStatus.CAPTURED ? "Auto-approved payment" : "Amount exceeds auto-approval limit")
                .createdAt(Instant.now())
                .build();
    }

    private void publishPaymentEvent(Payment payment) {
        String eventType = payment.getStatus() == PaymentStatus.CAPTURED ? "PaymentCompleted" : "PaymentFailed";
        paymentEventPublisher.publish(eventType, payment);
    }
}

