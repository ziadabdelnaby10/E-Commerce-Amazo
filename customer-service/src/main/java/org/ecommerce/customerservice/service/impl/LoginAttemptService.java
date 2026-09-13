package org.ecommerce.customerservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;

/**
 * Redis-backed brute-force protection for the login endpoint.
 *
 * <p>Without this, an attacker can try passwords as fast as BCrypt allows, and because the platform
 * runs multiple customer-service replicas an in-memory counter would be trivially bypassed by
 * spreading attempts across instances. Redis gives every replica a shared view of the counter.</p>
 *
 * <p>The counter is keyed by email and expires on a sliding window, so a legitimate user who
 * mistypes their password a few times is unblocked automatically without operator intervention.</p>
 */
@Slf4j
@Component
public class LoginAttemptService {

    static final String KEY_PREFIX = "customer:login-attempts:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration window;

    public LoginAttemptService(
            StringRedisTemplate redisTemplate,
            @Value("${application.security.login.max-attempts:5}") int maxAttempts,
            @Value("${application.security.login.lockout-window:15m}") Duration window) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    /**
     * @throws ResponseStatusException 429 when the caller has exhausted their attempts
     */
    public void assertNotLocked(String email) {
        String attempts;
        try {
            attempts = redisTemplate.opsForValue().get(key(email));
        } catch (Exception ex) {
            // Fail open: a Redis outage must not lock every customer out of the platform. The
            // password check itself still runs, so this only removes rate limiting, not authentication.
            log.warn("Login throttling unavailable; allowing attempt for {}", email, ex);
            return;
        }
        if (attempts != null && Integer.parseInt(attempts) >= maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts; try again later");
        }
    }

    public void recordFailure(String email) {
        try {
            String key = key(email);
            Long attempts = redisTemplate.opsForValue().increment(key);
            // Set the TTL on first failure only, so the window starts at the first bad attempt rather
            // than sliding forward forever while an attacker keeps guessing.
            if (attempts != null && attempts == 1L) {
                redisTemplate.expire(key, window);
            }
        } catch (Exception ex) {
            log.warn("Could not record failed login attempt for {}", email, ex);
        }
    }

    public void recordSuccess(String email) {
        try {
            redisTemplate.delete(key(email));
        } catch (Exception ex) {
            log.warn("Could not clear login attempts for {}", email, ex);
        }
    }

    private String key(String email) {
        return KEY_PREFIX + email.toLowerCase(Locale.ROOT);
    }
}


