package org.ecommerce.paymentservice.infrastructure.persistence.repository;

import org.ecommerce.paymentservice.domain.model.Payment;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;
import org.ecommerce.paymentservice.infrastructure.persistence.projection.PaymentSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = "transactions")
    @Query("select p from Payment p where p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdWithTransactions(String paymentId);

    @Query("""
            select p.paymentId as paymentId,
                   p.orderId as orderId,
                   p.status as status,
                   p.amount as amount,
                   p.currency as currency,
                   p.createdAt as createdAt
            from Payment p
            where p.userId = :userId
            order by p.createdAt desc
            """)
    Page<PaymentSummaryProjection> findSummariesByUser(String userId, Pageable pageable);

    @Query("""
            select p.paymentId as paymentId,
                   p.orderId as orderId,
                   p.status as status,
                   p.amount as amount,
                   p.currency as currency,
                   p.createdAt as createdAt
            from Payment p
            where p.userId = :userId
              and p.status = :status
            order by p.createdAt desc
            """)
    Page<PaymentSummaryProjection> findSummariesByUserAndStatus(String userId, PaymentStatus status, Pageable pageable);
}

