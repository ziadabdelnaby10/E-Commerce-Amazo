package org.ecommerce.notificationservice.infrastructure.persistence.repository;

import org.ecommerce.notificationservice.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(Long userId);
}

