package org.ecommerce.orderservice.infrastructure.persistence.repository;

import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.infrastructure.persistence.projection.OrderSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithItems(Long id);

    @Query("""
            select o.id as id,
                   o.orderNumber as orderNumber,
                   o.status as status,
                   o.paymentStatus as paymentStatus,
                   o.totalAmount as totalAmount,
                   o.currency as currency,
                   o.createdAt as createdAt
            from Order o
            where o.userId = :userId
            order by o.createdAt desc
            """)
    Page<OrderSummaryProjection> findSummariesByUser(Long userId, Pageable pageable);

    @Query("""
            select o.id as id,
                   o.orderNumber as orderNumber,
                   o.status as status,
                   o.paymentStatus as paymentStatus,
                   o.totalAmount as totalAmount,
                   o.currency as currency,
                   o.createdAt as createdAt
            from Order o
            where o.userId = :userId
              and o.status = :status
            order by o.createdAt desc
            """)
    Page<OrderSummaryProjection> findSummariesByUserAndStatus(Long userId, OrderStatus status, Pageable pageable);
}

