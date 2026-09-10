package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;

public interface CustomerAuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);
}

