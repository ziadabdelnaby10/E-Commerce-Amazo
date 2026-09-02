package org.ecommerce.paymentservice.infrastructure.persistence.repository;

import org.ecommerce.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransaction, Long> {
}

