package org.ecommerce.inventoryservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ecommerce.inventoryservice.model.response.GeneralErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Returns the project-wide JSON error envelope for unauthenticated requests
 * (missing, malformed, expired or otherwise invalid bearer tokens).
 *
 * <p>Authentication failures happen inside the security filter chain, before any controller runs,
 * so they cannot be handled by {@code @RestControllerAdvice}.</p>
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired access token";
    static final String UNAUTHENTICATED_MESSAGE = "Authentication is required to access this resource";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        String message = authenticationException instanceof InvalidBearerTokenException
                ? INVALID_TOKEN_MESSAGE
                : UNAUTHENTICATED_MESSAGE;

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                GeneralErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), message));
    }
}

