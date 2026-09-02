package org.ecommerce.notificationservice.infrastructure.persistence.repository;

import org.ecommerce.notificationservice.domain.model.Notification;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationType;
import org.ecommerce.notificationservice.infrastructure.persistence.projection.NotificationSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "logs")
    @Query("select n from Notification n where n.notificationId = :notificationId")
    Optional<Notification> findByNotificationIdWithLogs(String notificationId);

    @Query("""
            select n.id as id,
                   n.notificationId as notificationId,
                   n.userId as userId,
                   n.type as type,
                   n.status as status,
                   n.subject as subject,
                   n.recipientAddress as recipientAddress,
                   n.priority as priority,
                   n.retryCount as retryCount,
                   n.createdAt as createdAt,
                   n.sentAt as sentAt,
                   n.failedAt as failedAt
            from Notification n
            where n.userId = :userId
            order by n.createdAt desc
            """)
    Page<NotificationSummaryProjection> findSummariesByUser(Long userId, Pageable pageable);

    @Query("""
            select n.id as id,
                   n.notificationId as notificationId,
                   n.userId as userId,
                   n.type as type,
                   n.status as status,
                   n.subject as subject,
                   n.recipientAddress as recipientAddress,
                   n.priority as priority,
                   n.retryCount as retryCount,
                   n.createdAt as createdAt,
                   n.sentAt as sentAt,
                   n.failedAt as failedAt
            from Notification n
            where n.userId = :userId
              and n.type = :type
            order by n.createdAt desc
            """)
    Page<NotificationSummaryProjection> findSummariesByUserAndType(Long userId, NotificationType type, Pageable pageable);

    @Query("""
            select n.id as id,
                   n.notificationId as notificationId,
                   n.userId as userId,
                   n.type as type,
                   n.status as status,
                   n.subject as subject,
                   n.recipientAddress as recipientAddress,
                   n.priority as priority,
                   n.retryCount as retryCount,
                   n.createdAt as createdAt,
                   n.sentAt as sentAt,
                   n.failedAt as failedAt
            from Notification n
            where n.userId = :userId
              and n.status = :status
            order by n.createdAt desc
            """)
    Page<NotificationSummaryProjection> findSummariesByUserAndStatus(Long userId, NotificationStatus status, Pageable pageable);
}

