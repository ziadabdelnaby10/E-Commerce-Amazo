package org.ecommerce.paymentservice.infrastructure.persistence.repository;

import org.ecommerce.paymentservice.domain.model.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditLogJpaRepository extends JpaRepository<PaymentAuditLog, Long> {
}

