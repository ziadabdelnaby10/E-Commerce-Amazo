package org.ecommerce.customerservice.service.impl;

import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.config.JwtSecurityProperties;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.entity.RefreshToken;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.repository.RefreshTokenRepository;
import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;
import org.ecommerce.customerservice.service.JwtTokenService;
import org.ecommerce.customerservice.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtSecurityProperties jwtSecurityProperties;

    @InjectMocks
    private CustomerAuthServiceImpl authService;

    private LoginRequest loginRequest;
    private Customer customer;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("ziad@example.com", "SecurePassword123");
        customer = TestDataFactory.customer();
    }

    @Test
    void login_shouldIssueAccessAndRefreshTokenAndPersistRefreshToken() {
        Instant accessExpiresAt = Instant.parse("2026-09-08T12:30:00Z");
        when(jwtSecurityProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(30));
        when(customerRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(loginRequest.email())).thenReturn(Optional.of(customer));
        when(passwordService.isPasswordValid(loginRequest.password(), customer.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.generateAccessToken(customer)).thenReturn(new JwtTokenService.JwtToken("access-token", accessExpiresAt));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshTokenExpiresAt()).isAfter(Instant.now());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.userId()).isEqualTo(customer.getId());
        assertThat(response.email()).isEqualTo(customer.getEmail());
        assertThat(response.expiresAt()).isEqualTo(accessExpiresAt);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken savedRefreshToken = captor.getValue();
        assertThat(savedRefreshToken.getCustomer()).isEqualTo(customer);
        assertThat(savedRefreshToken.getTokenHash()).isEqualTo(hashToken(response.refreshToken()));
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(response.refreshTokenExpiresAt());
        assertThat(savedRefreshToken.getIsRevoked()).isFalse();
    }

    @Test
    void refresh_shouldRotateRefreshTokenAndIssueNewTokens() {
        String rawRefreshToken = "refresh-token-value";
        String hashedRefreshToken = hashToken(rawRefreshToken);
        RefreshToken existingRefreshToken = RefreshToken.builder()
                .id(1L)
                .customer(customer)
                .tokenHash(hashedRefreshToken)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Instant accessExpiresAt = Instant.parse("2026-09-08T12:45:00Z");

        when(jwtSecurityProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(30));
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalseAndExpiresAtAfter(eq(hashedRefreshToken), any(Instant.class)))
                .thenReturn(Optional.of(existingRefreshToken));
        when(jwtTokenService.generateAccessToken(customer)).thenReturn(new JwtTokenService.JwtToken("next-access-token", accessExpiresAt));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshTokenRequest(rawRefreshToken));

        assertThat(response.accessToken()).isEqualTo("next-access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo(rawRefreshToken);
        assertThat(response.refreshTokenExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().getFirst().getIsRevoked()).isTrue();
        assertThat(captor.getAllValues().getFirst().getRevokedAt()).isNotNull();
        assertThat(captor.getAllValues().getFirst().getLastUsedAt()).isNotNull();
        assertThat(captor.getAllValues().get(1).getIsRevoked()).isFalse();
        assertThat(captor.getAllValues().get(1).getTokenHash()).isEqualTo(hashToken(response.refreshToken()));
    }

    @Test
    void refresh_shouldThrowUnauthorizedWhenRefreshTokenIsUnknown() {
        String rawRefreshToken = "missing-refresh-token";
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalseAndExpiresAtAfter(eq(hashToken(rawRefreshToken)), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawRefreshToken)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    private String hashToken(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}


