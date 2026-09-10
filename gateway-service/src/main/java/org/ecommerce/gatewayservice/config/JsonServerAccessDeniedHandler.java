package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action";

    private final ObjectMapper objectMapper;

    public JsonServerAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException accessDeniedException) {
        return JsonServerAuthenticationEntryPoint.JsonErrorResponseWriter
                .write(exchange, objectMapper, HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE);
    }
}

