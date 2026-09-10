package org.ecommerce.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.customerservice.handler.GlobalExceptionHandler;
import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;
import org.ecommerce.customerservice.service.CustomerAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CustomerAuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(CustomerAuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void login_shouldReturnAccessAndRefreshTokens() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                Instant.parse("2026-09-08T12:30:00Z"),
                Instant.parse("2026-10-08T12:30:00Z"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ziad@example.com",
                List.of("ROLE_USER"),
                List.of("VIEW_ORDERS")
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ziad@example.com", "SecurePassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.refreshTokenExpiresAt").isString());
    }

    @Test
    void refresh_shouldReturnNewTokens() throws Exception {
        AuthResponse response = new AuthResponse(
                "next-access-token",
                "next-refresh-token",
                "Bearer",
                Instant.parse("2026-09-08T12:30:00Z"),
                Instant.parse("2026-10-08T12:30:00Z"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ziad@example.com",
                List.of("ROLE_USER"),
                List.of("VIEW_ORDERS")
        );
        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("next-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("next-refresh-token"));
    }
}

