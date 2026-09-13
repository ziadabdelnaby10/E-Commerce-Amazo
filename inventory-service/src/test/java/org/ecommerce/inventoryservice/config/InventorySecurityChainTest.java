package org.ecommerce.inventoryservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.ecommerce.inventoryservice.controller.InventoryController;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.ecommerce.inventoryservice.service.ProductService;
import org.ecommerce.inventoryservice.service.StockReservationCoordinator;
import org.ecommerce.inventoryservice.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the resource-server security chain: only valid, non-expired tokens carrying the required
 * authority may reach the controller, and every rejection is rendered with the shared JSON envelope.
 */
@WebMvcTest(controllers = InventoryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "application.security.enabled=true",
        "application.security.jwt.secret=" + InventorySecurityChainTest.SECRET,
        "application.security.jwt.issuer=" + InventorySecurityChainTest.ISSUER
})
class InventorySecurityChainTest {

    static final String SECRET = "test-secret-value-that-is-long-enough-for-hmac-sha256!!";
    static final String ISSUER = "ecommerce-platform";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private StockReservationCoordinator stockReservationCoordinator;

    @Test
    void request_withoutToken_isRejectedWithJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/inventory/product"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.UNAUTHENTICATED_MESSAGE));
    }

    @Test
    void request_withExpiredToken_isRejectedWithJsonUnauthorized() throws Exception {
        String token = token(Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS),
                ISSUER,
                List.of("VIEW_INVENTORY"));

        mockMvc.perform(get("/v1/inventory/product").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAuthenticationEntryPoint.INVALID_TOKEN_MESSAGE));
    }

    @Test
    void request_withWrongIssuer_isRejectedWithJsonUnauthorized() throws Exception {
        String token = token(Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS),
                "rogue-issuer",
                List.of("VIEW_INVENTORY"));

        mockMvc.perform(get("/v1/inventory/product").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    void request_withoutRequiredAuthority_isRejectedWithJsonForbidden() throws Exception {
        String token = token(Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS),
                ISSUER,
                List.of("VIEW_ORDERS"));

        mockMvc.perform(get("/v1/inventory/product").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(403))
                .andExpect(jsonPath("$.errorDescription")
                        .value(RestAccessDeniedHandler.ACCESS_DENIED_MESSAGE));
    }

    @Test
    void request_withValidTokenAndAuthority_reachesController() throws Exception {
        String token = token(Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS),
                ISSUER,
                List.of("VIEW_INVENTORY"));

        mockMvc.perform(get("/v1/inventory/product").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoints_remainAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 401 || statusCode == 403) {
                        throw new AssertionError("Health endpoint must stay public but returned " + statusCode);
                    }
                });
    }

    static String token(Instant issuedAt, Instant expiresAt, String issuer, List<String> permissions) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject("tester@example.com")
                .claim("userId", "42")
                .claim("roles", List.of("CUSTOMER"))
                .claim("permissions", permissions)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}




