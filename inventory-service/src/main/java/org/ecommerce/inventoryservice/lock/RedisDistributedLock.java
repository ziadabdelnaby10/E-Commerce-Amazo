package org.ecommerce.inventoryservice.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cross-instance mutual exclusion backed by Redis.
 *
 * <p>Stock reservation is a check-then-act sequence: it reads the available quantity, decides
 * whether it is sufficient, then writes the new value. With more than one inventory-service replica
 * two concurrent reservations can both pass the check and oversell the product. A database row lock
 * would only help within a single transaction boundary, so the mutual exclusion is pushed out to
 * Redis where every replica can see it.</p>
 *
 * <p>Locks are acquired with {@code SET key value NX PX ttl}: atomic acquisition plus a TTL that
 * guarantees the lock is eventually released even if the holder crashes. Release is done with a Lua
 * script that compares the stored token before deleting, so a process whose lock already expired can
 * never delete a lock now held by someone else.</p>
 */
@Slf4j
@Component
public class RedisDistributedLock {

    static final String KEY_PREFIX = "inventory:lock:";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);
    private static final Duration ACQUIRE_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(50);

    /**
     * Deletes the key only when it still holds our token, making release safe against the case where
     * our TTL expired and another holder acquired the same key in the meantime.
     */
    private static final RedisScript<Long> RELEASE_IF_OWNED = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Runs {@code action} while holding a lock on every supplied key.
     *
     * <p>Keys are sorted before acquisition so that two callers requesting an overlapping set of
     * products always take them in the same order, which makes deadlock impossible.</p>
     *
     * @throws LockAcquisitionException if any lock cannot be acquired within the timeout
     */
    public <T> T executeLocked(Collection<String> keys, Supplier<T> action) {
        List<String> orderedKeys = keys.stream().distinct().sorted().toList();
        String token = UUID.randomUUID().toString();
        List<String> acquired = new ArrayList<>(orderedKeys.size());

        try {
            for (String key : orderedKeys) {
                if (!acquire(key, token)) {
                    throw new LockAcquisitionException(key);
                }
                acquired.add(key);
            }
            return action.get();
        } finally {
            // Release in reverse order so the most recently acquired lock is freed first.
            for (int i = acquired.size() - 1; i >= 0; i--) {
                release(acquired.get(i), token);
            }
        }
    }

    private boolean acquire(String key, String token) {
        String redisKey = KEY_PREFIX + key;
        long deadline = System.nanoTime() + ACQUIRE_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, token, DEFAULT_TTL);
            if (Boolean.TRUE.equals(locked)) {
                return true;
            }
            try {
                Thread.sleep(RETRY_BACKOFF.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void release(String key, String token) {
        try {
            redisTemplate.execute(RELEASE_IF_OWNED, List.of(KEY_PREFIX + key), token);
        } catch (Exception ex) {
            // The TTL guarantees the lock is freed anyway, so a failed release must not mask the
            // outcome of the guarded action.
            log.warn("Failed to release inventory lock for key {}", key, ex);
        }
    }
}


