package org.ecommerce.customerservice.service;

import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.config.JwtSecurityProperties;
import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.entity.Permission;
import org.ecommerce.customerservice.entity.Role;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates signed access tokens from authenticated customer entities.
 *
 * <p>The generated JWT contains identity, role, and permission claims so downstream
 * services can authorize requests without calling customer-service for every access.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties jwtSecurityProperties;

    /**
     * Generates a signed access token for the supplied customer.
     *
     * @param customer authenticated customer entity
     * @return token value plus expiration metadata
     */
    public JwtToken generateAccessToken(Customer customer) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtSecurityProperties.accessTokenTtl());
        List<String> roles = extractRoles(customer);
        List<String> permissions = extractPermissions(customer);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtSecurityProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(customer.getEmail())
                .claim("userId", customer.getId() == null ? "" : customer.getId().toString())
                .claim("email", customer.getEmail())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("isActive", customer.getIsActive())
                .build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new JwtToken(tokenValue, expiresAt);
    }

    /** @return logical role names carried by the customer */
    private List<String> extractRoles(Customer customer) {
        return customer.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    /** @return distinct permissions inherited through the customer's roles */
    private List<String> extractPermissions(Customer customer) {
        return customer.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Lightweight representation of a freshly issued access token.
     *
     * @param tokenValue serialized JWT value
     * @param expiresAt absolute token expiration instant
     */
    public record JwtToken(String tokenValue, Instant expiresAt) {
        /** @return remaining token lifetime in whole seconds */
        public long expiresInSeconds() {
            return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        }
    }
}



