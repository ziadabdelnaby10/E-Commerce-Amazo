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

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties jwtSecurityProperties;

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

    private List<String> extractRoles(Customer customer) {
        return customer.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    private List<String> extractPermissions(Customer customer) {
        return customer.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    public record JwtToken(String tokenValue, Instant expiresAt) {
        public long expiresInSeconds() {
            return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        }
    }
}



