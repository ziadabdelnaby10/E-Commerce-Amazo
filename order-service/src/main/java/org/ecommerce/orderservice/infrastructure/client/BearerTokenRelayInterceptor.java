package org.ecommerce.orderservice.infrastructure.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Relays the caller's validated access token to downstream services.
 *
 * <p>Every downstream service is an independent OAuth2 resource server, so an outbound call without
 * an {@code Authorization} header is rejected with 401. Rather than minting a second token, the
 * original caller's credential is forwarded so downstream authorization decisions are still made
 * against the real end user.</p>
 *
 * <p>The token is read from the {@link JwtAuthenticationToken} in the security context - never from
 * a request header - so only signature-verified, unexpired tokens are ever propagated.</p>
 *
 * <p>Requests with no authenticated JWT (for example the scheduled outbox publisher) are left
 * untouched; the downstream service will reject them, which is the intended behaviour.</p>
 */
@Slf4j
@Configuration
public class BearerTokenRelayInterceptor implements RequestInterceptor {

    static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
            return;
        }

        currentTokenValue().ifPresentOrElse(
                token -> template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token),
                () -> log.debug("No authenticated JWT available to relay to {}", template.feignTarget().name()));
    }

    private Optional<String> currentTokenValue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(jwtAuthentication.getToken().getTokenValue());
        }
        return Optional.empty();
    }
}



