package org.ecommerce.customerservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.config.JwtSecurityProperties;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.entity.RefreshToken;
import org.ecommerce.customerservice.entity.Permission;
import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.repository.RefreshTokenRepository;
import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;
import org.ecommerce.customerservice.service.CustomerAuthService;
import org.ecommerce.customerservice.service.JwtTokenService;
import org.ecommerce.customerservice.service.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final JwtSecurityProperties jwtSecurityProperties;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
                .orElseThrow(this::unauthorized);

        if (!passwordService.isPasswordValid(request.password(), customer.getPasswordHash())) {
            throw unauthorized();
        }

        customer.setLastLoginAt(Instant.now());
        customerRepository.save(customer);

        JwtTokenService.JwtToken token = jwtTokenService.generateAccessToken(customer);
        IssuedRefreshToken refreshToken = issueRefreshToken(customer);

        return buildAuthResponse(customer, token, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndIsRevokedFalseAndExpiresAtAfter(hashToken(request.refreshToken()), Instant.now())
                .orElseThrow(this::invalidRefreshToken);

        Customer customer = refreshToken.getCustomer();
        if (Boolean.FALSE.equals(customer.getIsActive()) || customer.getDeletedAt() != null) {
            throw invalidRefreshToken();
        }

        Instant now = Instant.now();
        refreshToken.setIsRevoked(true);
        refreshToken.setRevokedAt(now);
        refreshToken.setLastUsedAt(now);
        refreshTokenRepository.save(refreshToken);

        JwtTokenService.JwtToken token = jwtTokenService.generateAccessToken(customer);
        IssuedRefreshToken nextRefreshToken = issueRefreshToken(customer);

        return buildAuthResponse(customer, token, nextRefreshToken);
    }

    private AuthResponse buildAuthResponse(Customer customer, JwtTokenService.JwtToken token, IssuedRefreshToken refreshToken) {
        List<String> roles = customer.getRoles().stream().map(Role::getName).toList();
        List<String> permissions = customer.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        return new AuthResponse(
                token.tokenValue(),
                refreshToken.tokenValue(),
                "Bearer",
                token.expiresAt(),
                refreshToken.expiresAt(),
                customer.getId(),
                customer.getEmail(),
                roles,
                permissions
        );
    }

    private IssuedRefreshToken issueRefreshToken(Customer customer) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtSecurityProperties.refreshTokenTtl());
        String rawToken = generateTokenValue();

        refreshTokenRepository.save(RefreshToken.builder()
                .customer(customer)
                .tokenHash(hashToken(rawToken))
                .expiresAt(expiresAt)
                .build());

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    private String generateTokenValue() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    private record IssuedRefreshToken(String tokenValue, Instant expiresAt) {
    }
}


