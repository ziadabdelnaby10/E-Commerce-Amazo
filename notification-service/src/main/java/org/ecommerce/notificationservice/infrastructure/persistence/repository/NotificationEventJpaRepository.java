package org.ecommerce.notificationservice.infrastructure.persistence.repository;

import org.ecommerce.notificationservice.domain.model.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventJpaRepository extends JpaRepository<NotificationEvent, Long> {
    boolean existsByEventId(String eventId);
}

