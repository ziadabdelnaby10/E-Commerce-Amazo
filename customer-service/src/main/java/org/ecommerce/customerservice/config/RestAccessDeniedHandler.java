package org.ecommerce.customerservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ecommerce.customerservice.response.GeneralErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Returns the project-wide JSON error envelope when an authenticated caller lacks the
 * authority required by a {@code @PreAuthorize} rule.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action";

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                GeneralErrorResponse.of(HttpStatus.FORBIDDEN.value(), ACCESS_DENIED_MESSAGE));
    }
}

