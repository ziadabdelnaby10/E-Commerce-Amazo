package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive equivalent of the servlet {@code AccessDeniedHandler}: renders a JSON body when an
 * authenticated caller is not allowed to reach the requested route.
 */
public class JsonServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    /** User-facing message returned for authorization failures. */
    static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action";

    /** Serializer used to render JSON error bodies. */
    private final ObjectMapper objectMapper;

    /**
     * Creates a new access denied handler.
     *
     * @param objectMapper serializer for JSON error bodies
     */
    public JsonServerAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a standardized 403 JSON response.
     *
     * @param exchange current reactive exchange
     * @param accessDeniedException authorization failure
     * @return asynchronous completion signal
     */
    @Override
    public @NotNull Mono<Void> handle(@NotNull ServerWebExchange exchange,
                                      @NotNull AccessDeniedException accessDeniedException) {
        return JsonServerAuthenticationEntryPoint.JsonErrorResponseWriter
                .write(exchange, objectMapper, HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE);
    }
}

