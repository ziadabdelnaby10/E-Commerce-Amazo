package org.ecommerce.notificationservice.infrastructure.persistence.repository;

import org.ecommerce.notificationservice.domain.model.NotificationTemplate;
import org.ecommerce.notificationservice.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByNameAndTypeAndActiveTrue(String name, NotificationType type);
}

