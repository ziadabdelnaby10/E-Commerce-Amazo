package org.ecommerce.orderservice.infrastructure.persistence.repository;

import org.ecommerce.orderservice.domain.model.IdempotencyKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    @Query("select k.id from IdempotencyKey k where k.expiresAt < :expiresAt order by k.expiresAt asc")
    List<Long> findExpiredIds(Instant expiresAt, Pageable pageable);
}

