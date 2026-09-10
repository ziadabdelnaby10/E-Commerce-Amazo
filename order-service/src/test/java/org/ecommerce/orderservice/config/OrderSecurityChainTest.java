package org.ecommerce.orderservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.ecommerce.orderservice.controller.OrderController;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.service.OrderService;
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
 * Guards the order-service security contract: authority mapping from the JWT claims, issuer and
 * expiry validation, per-user data isolation and the shared JSON error envelope.
 */
@WebMvcTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "application.config.customer-url=http://localhost:9001/api/v1/customers",
        "application.config.payment-url=http://localhost:9004/api/v1/payments",
        "application.config.inventory-url=http://localhost:9002/api/v1/inventory",
        "application.security.enabled=true",
        "application.security.jwt.secret=" + OrderSecurityChainTest.SECRET,
        "application.security.jwt.issuer=" + OrderSecurityChainTest.ISSUER
})
class OrderSecurityChainTest {

    static final String SECRET = "test-secret-value-that-is-long-enough-for-hmac-sha256!!";
    static final String ISSUER = "ecommerce-platform";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void request_withoutToken_isRejectedWithJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/orders").param("userId", "42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.UNAUTHENTICATED_MESSAGE));
    }

    @Test
    void request_withExpiredToken_isRejectedWithJsonUnauthorized() throws Exception {
        String token = token(Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS),
                ISSUER, "42", List.of("VIEW_ORDERS"));

        mockMvc.perform(get("/v1/orders").param("userId", "42").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.INVALID_TOKEN_MESSAGE));
    }

    @Test
    void request_withoutRequiredAuthority_isRejectedWithJsonForbidden() throws Exception {
        String token = validToken("42", List.of("CREATE_ORDER"));

        mockMvc.perform(get("/v1/orders").param("userId", "42").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(403))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAccessDeniedHandler.ACCESS_DENIED_MESSAGE));
    }

    @Test
    void listingAnotherUsersOrders_isForbidden() throws Exception {
        String token = validToken("42", List.of("VIEW_ORDERS"));

        mockMvc.perform(get("/v1/orders").param("userId", "99").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingOwnOrders_isAllowed() throws Exception {
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of());
        given(orderService.listByUser(eq(42L), any(), any())).willReturn(page);
        String token = validToken("42", List.of("VIEW_ORDERS"));

        mockMvc.perform(get("/v1/orders").param("userId", "42").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void administratorCanListAnyUsersOrders() throws Exception {
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of());
        given(orderService.listByUser(eq(99L), any(), any())).willReturn(page);
        String token = validToken("42", List.of("VIEW_ORDERS", "VIEW_USERS"));

        mockMvc.perform(get("/v1/orders").param("userId", "99").header("Authorization", "Bearer " + token))
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




