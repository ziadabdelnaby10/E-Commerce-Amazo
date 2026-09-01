package org.ecommerce.orderservice.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.ecommerce.orderservice.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private final IdempotencyKeyJpaRepository repository;

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void cleanupExpiredKeys() {
        List<Long> expiredIds = repository.findExpiredIds(Instant.now(), PageRequest.of(0, 500));
        if (expiredIds.isEmpty()) {
            return;
        }

        repository.deleteAllByIdInBatch(expiredIds);
        log.info("Deleted {} expired idempotency keys", expiredIds.size());
    }
}

