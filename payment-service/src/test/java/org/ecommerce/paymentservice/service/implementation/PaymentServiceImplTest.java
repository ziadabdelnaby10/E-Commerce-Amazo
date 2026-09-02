package org.ecommerce.paymentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.InitiatePaymentResponse;
import org.ecommerce.paymentservice.domain.model.Payment;
import org.ecommerce.paymentservice.domain.model.PaymentMethodType;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;
import org.ecommerce.paymentservice.infrastructure.mapping.PaymentMapper;
import org.ecommerce.paymentservice.infrastructure.messaging.PaymentEventPublisher;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentAuditLogJpaRepository;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentJpaRepository;
import org.ecommerce.paymentservice.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentJpaRepository paymentRepository;
    @Mock
    private PaymentTransactionJpaRepository paymentTransactionRepository;
    @Mock
    private PaymentAuditLogJpaRepository paymentAuditLogRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentTransactionRepository,
                paymentAuditLogRepository,
                paymentMapper,
                paymentEventPublisher,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnExistingPaymentForIdempotentOrder() {
        InitiatePaymentRequest request = new InitiatePaymentRequest(10L, "user-10", BigDecimal.TEN, "USD");
        Payment existing = Payment.builder()
                .paymentId("PAY-EXISTING")
                .orderId(10L)
                .userId("user-10")
                .amount(BigDecimal.TEN)
                .currency("USD")
                .status(PaymentStatus.CAPTURED)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(existing));
        when(paymentMapper.toInitiateResponse(existing))
                .thenReturn(new InitiatePaymentResponse("PAY-EXISTING", "AUTHORIZED", null));

        InitiatePaymentResponse response = paymentService.initiatePayment(request);

        assertEquals("PAY-EXISTING", response.paymentId());
        assertEquals("AUTHORIZED", response.status());
        assertNull(response.reason());
        verify(paymentRepository, times(1)).findTopByOrderIdOrderByCreatedAtDesc(10L);
        verify(paymentMapper, times(1)).toInitiateResponse(existing);
    }

    @Test
    void shouldCreateAndPublishCompletedPaymentWhenAmountWithinLimit() {
        InitiatePaymentRequest request = new InitiatePaymentRequest(77L, "user-77", BigDecimal.valueOf(99.99), "USD");
        Payment mapped = Payment.builder()
                .orderId(77L)
                .userId("user-77")
                .amount(BigDecimal.valueOf(99.99))
                .build();

        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(77L)).thenReturn(Optional.empty());
        when(paymentMapper.toPayment(request)).thenReturn(mapped);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InitiatePaymentResponse response = paymentService.initiatePayment(request);

        assertEquals("AUTHORIZED", response.status());
        assertNull(response.reason());

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(paymentEventPublisher).publish(eventTypeCaptor.capture(), any(Payment.class));
        assertEquals("PaymentCompleted", eventTypeCaptor.getValue());
        verify(paymentTransactionRepository, times(1)).save(any());
        verify(paymentAuditLogRepository, times(1)).save(any());
        verify(paymentMapper, times(1)).toPayment(eq(request));
    }
}

