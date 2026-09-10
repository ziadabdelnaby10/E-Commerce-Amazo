package org.ecommerce.notificationservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.ecommerce.notificationservice.api.NotificationController;
import org.ecommerce.notificationservice.api.dto.NotificationSummaryResponse;
import org.ecommerce.notificationservice.application.service.NotificationApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the notification-service security contract: authority mapping, expiry validation, per-user
 * inbox isolation and the shared JSON error envelope.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "application.security.enabled=true",
        "application.security.jwt.secret=" + NotificationSecurityChainTest.SECRET,
        "application.security.jwt.issuer=" + NotificationSecurityChainTest.ISSUER
})
class NotificationSecurityChainTest {

    static final String SECRET = "test-secret-value-that-is-long-enough-for-hmac-sha256!!";
    static final String ISSUER = "ecommerce-platform";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationApplicationService notificationService;

    @Test
    void request_withoutToken_isRejectedWithJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/notifications").param("userId", "42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.UNAUTHENTICATED_MESSAGE));
    }

    @Test
    void request_withExpiredToken_isRejectedWithJsonUnauthorized() throws Exception {
        String token = token(Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS), ISSUER, "42", List.of("VIEW_USERS"));

        mockMvc.perform(get("/v1/notifications").param("userId", "42").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.INVALID_TOKEN_MESSAGE));
    }

    @Test
    void readingAnotherUsersInbox_isForbidden() throws Exception {
        String token = validToken("42", List.of());

        mockMvc.perform(get("/v1/notifications").param("userId", "99").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(403))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAccessDeniedHandler.ACCESS_DENIED_MESSAGE));
    }

    @Test
    void readingOwnInbox_isAllowed() throws Exception {
        Page<NotificationSummaryResponse> page = new PageImpl<>(List.of());
        given(notificationService.getInbox(eq(42L), any(), any(), any())).willReturn(page);
        String token = validToken("42", List.of());

        mockMvc.perform(get("/v1/notifications").param("userId", "42").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void administratorCanReadAnyInbox() throws Exception {
        Page<NotificationSummaryResponse> page = new PageImpl<>(List.of());
        given(notificationService.getInbox(eq(99L), any(), any(), any())).willReturn(page);
        String token = validToken("42", List.of("VIEW_USERS"));

        mockMvc.perform(get("/v1/notifications").param("userId", "99").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private static String validToken(String userId, List<String> permissions) {
        return token(Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), ISSUER, userId, permissions);
    }

    static String token(Instant issuedAt, Instant expiresAt, String issuer, String userId, List<String> permissions) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject("tester@example.com")
                .claim("userId", userId)
                .claim("roles", List.of("CUSTOMER"))
                .claim("permissions", permissions)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}

