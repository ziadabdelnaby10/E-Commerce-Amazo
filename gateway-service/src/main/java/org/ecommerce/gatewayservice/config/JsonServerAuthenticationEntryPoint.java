package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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

    /** Message returned when the supplied access token is invalid or expired. */
    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired access token";
    /** Generic message returned when no valid authentication is present. */
    static final String UNAUTHENTICATED_MESSAGE = "Authentication is required to access this resource";

    /** Serializer used to render the JSON error payload. */
    private final ObjectMapper objectMapper;

    /**
     * Creates a new entry point.
     *
     * @param objectMapper serializer for JSON error bodies
     */
    public JsonServerAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a standardized 401 JSON response.
     *
     * @param exchange current reactive exchange
     * @param authenticationException security exception that triggered the failure
     * @return asynchronous completion signal
     */
    @Override
    public @NotNull Mono<Void> commence(@NotNull ServerWebExchange exchange,
                                        @NotNull AuthenticationException authenticationException) {
        String message = authenticationException instanceof InvalidBearerTokenException
                ? INVALID_TOKEN_MESSAGE
                : UNAUTHENTICATED_MESSAGE;
        return JsonErrorResponseWriter.write(exchange, objectMapper, HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Shared helper used by both authentication and authorization error handlers.
     */
    static final class JsonErrorResponseWriter {

        /**
         * Utility class; no instances allowed.
         */
        private JsonErrorResponseWriter() {
        }

        /**
         * Writes a compact JSON error payload to the reactive response.
         *
         * <p>A minimal string-based fallback is used if JSON serialization itself fails,
         * ensuring the client still receives a valid error response.</p>
         *
         * @param exchange current exchange
         * @param objectMapper serializer for the response body
         * @param status HTTP status to return
         * @param message human-readable error description
         * @return asynchronous completion signal
         */
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
            body.put("time", Instant.now());

            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsBytes(body);
            } catch (Exception ex) {
                // Fallback path ensures authentication errors still return valid JSON.
                bytes = ("{\"errorCode\":" + status.value() + ",\"errorDescription\":\"" + message + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
            }
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}

