package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the gateway's edge behaviour: identity headers can never be forged by a client, claims
 * are relayed only from a validated token, and auth failures are rendered as JSON.
 */
class GatewayEdgeSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtClaimRelayGlobalFilter relayFilter = new JwtClaimRelayGlobalFilter();

    @Test
    void clientSuppliedIdentityHeaders_areStrippedWhenUnauthenticated() {
        ServerWebExchange forwarded = relay(spoofedRequest(), null);

        assertThat(forwarded.getRequest().getHeaders().getFirst(JwtClaimRelayGlobalFilter.USER_ID_HEADER)).isNull();
        assertThat(forwarded.getRequest().getHeaders().getFirst(JwtClaimRelayGlobalFilter.USER_ROLES_HEADER)).isNull();
    }

    @Test
    void claimsFromValidatedToken_replaceAnyClientSuppliedHeaders() {
        Authentication authentication = new JwtAuthenticationToken(jwt("42", List.of("CUSTOMER")));

        ServerWebExchange forwarded = relay(spoofedRequest(), authentication);

        assertThat(forwarded.getRequest().getHeaders().getFirst(JwtClaimRelayGlobalFilter.USER_ID_HEADER))
                .isEqualTo("42");
        assertThat(forwarded.getRequest().getHeaders().getFirst(JwtClaimRelayGlobalFilter.USER_ROLES_HEADER))
                .isEqualTo("CUSTOMER");
    }

    @Test
    void nonJwtPrincipal_doesNotProduceIdentityHeaders() {
        ServerWebExchange forwarded = relay(spoofedRequest(), new TestingAuthenticationToken("someone", "n/a"));

        assertThat(forwarded.getRequest().getHeaders().getFirst(JwtClaimRelayGlobalFilter.USER_ID_HEADER)).isNull();
    }

    @Test
    void expiredToken_isRenderedAsJsonUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));

        StepVerifier.create(new JsonServerAuthenticationEntryPoint(objectMapper)
                        .commence(exchange, new InvalidBearerTokenException("Jwt expired")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"errorCode\":401")
                .contains(JsonServerAuthenticationEntryPoint.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void accessDenied_isRenderedAsJsonForbidden() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));

        StepVerifier.create(new JsonServerAccessDeniedHandler(objectMapper)
                        .handle(exchange, new AccessDeniedException("denied")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"errorCode\":403")
                .contains(JsonServerAccessDeniedHandler.ACCESS_DENIED_MESSAGE);
    }

    private MockServerWebExchange spoofedRequest() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header(JwtClaimRelayGlobalFilter.USER_ID_HEADER, "spoofed-user")
                .header(JwtClaimRelayGlobalFilter.USER_ROLES_HEADER, "ADMIN"));
    }

    private ServerWebExchange relay(MockServerWebExchange exchange, Authentication authentication) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = captured -> {
            forwarded.set(captured);
            return Mono.empty();
        };

        StepVerifier.create(relayFilter.filter(new AuthenticatedExchange(exchange, authentication), chain))
                .verifyComplete();

        return forwarded.get();
    }

    private static Jwt jwt(String userId, List<String> roles) {
        Instant issuedAt = Instant.now();
        return new Jwt("token-value",
                issuedAt,
                issuedAt.plus(1, ChronoUnit.HOURS),
                Map.of("alg", "HS256"),
                Map.of("sub", "tester@example.com", "userId", userId, "roles", roles));
    }

    /** Exposes a pre-authenticated principal, as the security filter chain would do at runtime. */
    private static final class AuthenticatedExchange extends ServerWebExchangeDecorator {

        private final Authentication authentication;

        private AuthenticatedExchange(ServerWebExchange delegate, Authentication authentication) {
            super(delegate);
            this.authentication = authentication;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Principal> Mono<T> getPrincipal() {
            return authentication == null ? Mono.empty() : Mono.just((T) authentication);
        }
    }
}
