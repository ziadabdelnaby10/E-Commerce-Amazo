package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reactive equivalent of the servlet {@code AuthenticationEntryPoint}: renders a JSON body for
 * requests rejected by the WebFlux security filter chain (expired or invalid bearer tokens).
 *
 * <p>These failures occur before routing, so {@code @RestControllerAdvice} cannot intercept them.</p>
 */
public class JsonServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired access token";
    static final String UNAUTHENTICATED_MESSAGE = "Authentication is required to access this resource";

    private final ObjectMapper objectMapper;

    public JsonServerAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authenticationException) {
        String message = authenticationException instanceof InvalidBearerTokenException
                ? INVALID_TOKEN_MESSAGE
                : UNAUTHENTICATED_MESSAGE;
        return JsonErrorResponseWriter.write(exchange, objectMapper, HttpStatus.UNAUTHORIZED, message);
    }

    /** Shared JSON rendering used by both the entry point and the access denied handler. */
    static final class JsonErrorResponseWriter {

        private JsonErrorResponseWriter() {
        }

        static Mono<Void> write(ServerWebExchange exchange,
                                ObjectMapper objectMapper,
                                HttpStatus status,
                                String message) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("errorCode", status.value());
            body.put("errorDescription", message);
            body.put("time", Instant.now().toString());

            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsBytes(body);
            } catch (Exception ex) {
                bytes = ("{\"errorCode\":" + status.value() + ",\"errorDescription\":\"" + message + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
            }
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}

