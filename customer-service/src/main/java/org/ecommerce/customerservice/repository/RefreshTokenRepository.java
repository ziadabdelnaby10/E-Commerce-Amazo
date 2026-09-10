package org.ecommerce.customerservice.repository;

import org.ecommerce.customerservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndIsRevokedFalseAndExpiresAtAfter(String tokenHash, Instant now);
}

