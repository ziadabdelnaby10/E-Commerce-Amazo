package org.ecommerce.gatewayservice.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Propagates identity claims from the <em>validated</em> token to downstream services.
 *
 * <p>Two security rules are enforced here:</p>
 * <ul>
 *   <li>Inbound {@code X-User-*} headers are always stripped, so a client cannot forge an identity
 *       by sending them itself.</li>
 *   <li>Values are read from the authenticated {@link JwtAuthenticationToken} produced by the
 *       security filter chain, never from an unverified base64 decode of the raw token.</li>
 * </ul>
 *
 * <p>Downstream services must still treat these headers as a convenience only: they perform their
 * own JWT validation and derive identity from the token itself.</p>
 */
@Component
@ConditionalOnProperty(prefix = "application.security", name = "enabled", havingValue = "true")
public class JwtClaimRelayGlobalFilter implements GlobalFilter, Ordered {

    /** Header carrying the authenticated user's logical identifier. */
    static final String USER_ID_HEADER = "X-User-Id";
    /** Header carrying a comma-separated role list extracted from the JWT. */
    static final String USER_ROLES_HEADER = "X-User-Roles";

    /**
     * Adds relayed identity headers when an authenticated JWT principal exists.
     *
     * <p>If no authenticated JWT principal is present, the request is still cleaned of any
     * client-supplied identity headers before continuing through the chain.</p>
     *
     * @param exchange current request/response exchange
     * @param chain remaining gateway filter chain
     * @return asynchronous completion signal
     */
    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> withRelayedClaims(exchange, authentication.getToken()))
                .defaultIfEmpty(withoutClientSuppliedIdentityHeaders(exchange))
                .flatMap(chain::filter);
    }

    /**
     * Creates a mutated exchange with trusted identity headers derived from the JWT.
     *
     * @param exchange current gateway exchange
     * @param jwt already-validated JWT token
     * @return new exchange carrying relayed claim headers
     */
    private ServerWebExchange withRelayedClaims(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest.Builder request = strippedRequestBuilder(exchange);

        String userId = jwt.getClaimAsString("userId");
        if (userId != null && !userId.isBlank()) {
            request.header(USER_ID_HEADER, userId);
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            request.header(USER_ROLES_HEADER, String.join(",", roles));
        }

        return exchange.mutate().request(request.build()).build();
    }

    /**
     * Creates a mutated exchange that only removes any client-supplied identity headers.
     *
     * @param exchange current gateway exchange
     * @return sanitized exchange
     */
    private ServerWebExchange withoutClientSuppliedIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate().request(strippedRequestBuilder(exchange).build()).build();
    }

    /**
     * Returns a request builder with gateway-managed identity headers stripped.
     *
     * <p>This prevents a caller from forging identity context by manually injecting headers
     * intended to be owned by the gateway.</p>
     *
     * @param exchange current gateway exchange
     * @return mutable request builder without relayed identity headers
     */
    private ServerHttpRequest.Builder strippedRequestBuilder(ServerWebExchange exchange) {
        return exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                });
    }

    /**
     * Runs this filter late in the chain so it can rely on the authenticated principal already being available.
     *
     * @return Spring ordering value
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
