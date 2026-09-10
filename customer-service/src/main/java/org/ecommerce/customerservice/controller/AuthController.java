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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GeneralResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GeneralResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), authService.refresh(request)));
    }
}

