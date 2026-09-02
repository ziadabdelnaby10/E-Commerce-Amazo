package org.ecommerce.paymentservice.infrastructure.mapping;

import org.ecommerce.paymentservice.domain.dto.request.InitiatePaymentRequest;
import org.ecommerce.paymentservice.domain.dto.response.InitiatePaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentSummaryResponse;
import org.ecommerce.paymentservice.domain.dto.response.PaymentTransactionResponse;
import org.ecommerce.paymentservice.domain.model.Payment;
import org.ecommerce.paymentservice.domain.model.PaymentTransaction;
import org.ecommerce.paymentservice.infrastructure.persistence.projection.PaymentSummaryProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "paymentMethodId", ignore = true)
    @Mapping(target = "merchantReference", ignore = true)
    @Mapping(target = "gatewayResponse", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authorizedAt", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Payment toPayment(InitiatePaymentRequest request);

    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "reason", ignore = true)
    InitiatePaymentResponse toInitiateResponse(Payment payment);

    PaymentResponse toResponse(Payment payment);

    PaymentTransactionResponse toTransactionResponse(PaymentTransaction paymentTransaction);

    PaymentSummaryResponse toSummary(PaymentSummaryProjection projection);
}

