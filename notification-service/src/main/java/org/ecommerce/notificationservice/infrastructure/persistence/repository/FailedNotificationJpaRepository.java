package org.ecommerce.notificationservice.infrastructure.persistence.repository;

import org.ecommerce.notificationservice.domain.model.FailedNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface FailedNotificationJpaRepository extends JpaRepository<FailedNotification, Long> {

    @Query("""
            select fn
            from FailedNotification fn
            join fetch fn.notification n
            where fn.nextRetryTime is not null
              and fn.nextRetryTime <= :now
            order by fn.nextRetryTime asc
            """)
    List<FailedNotification> findReadyForRetry(Instant now, Pageable pageable);
}

