package org.ecommerce.customerservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;
import org.ecommerce.customerservice.response.GeneralResponse;
import org.ecommerce.customerservice.service.CustomerAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints for login and refresh-token rotation.
 *
 * <p>The controller stays intentionally thin: it validates incoming payloads, delegates
 * to the authentication service, and wraps successful results in the project's generic
 * response envelope.</p>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerAuthService authService;

    /**
     * Authenticates a customer and returns an access/refresh token pair.
     *
     * @param request login payload containing email and password
     * @return authentication result wrapped in the standard response envelope
     */
    @PostMapping("/login")
    public ResponseEntity<GeneralResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), authService.login(request)));
    }

    /**
     * Exchanges a valid refresh token for a newly issued token pair.
     *
     * @param request refresh request carrying the previously issued refresh token
     * @return rotated authentication result wrapped in the standard response envelope
     */
    @PostMapping("/refresh")
    public ResponseEntity<GeneralResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), authService.refresh(request)));
    }
}

