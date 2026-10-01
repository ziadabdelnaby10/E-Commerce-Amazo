package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.request.LoginRequest;
import org.ecommerce.customerservice.request.RefreshTokenRequest;
import org.ecommerce.customerservice.response.AuthResponse;

/**
 * Authentication use cases exposed by customer-service.
 */
public interface CustomerAuthService {

    /**
     * Authenticates a customer and issues a new token pair.
     *
     * @param request login payload
     * @return token response enriched with identity and authorization data
     */
    AuthResponse login(LoginRequest request);

    /**
     * Rotates a refresh token and issues a fresh access/refresh pair.
     *
     * @param request refresh payload
     * @return newly issued token response
     */
    AuthResponse refresh(RefreshTokenRequest request);
}

