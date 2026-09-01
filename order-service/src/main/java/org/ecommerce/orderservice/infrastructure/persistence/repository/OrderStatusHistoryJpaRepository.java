package org.ecommerce.orderservice.infrastructure.persistence.repository;

import org.ecommerce.orderservice.domain.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryJpaRepository extends JpaRepository<OrderStatusHistory, Long> {
}

