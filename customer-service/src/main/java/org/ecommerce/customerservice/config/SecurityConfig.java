package org.ecommerce.customerservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Servlet-based Spring Security configuration for customer-service.
 *
 * <p>This service both issues JWTs and validates them for protected customer-management
 * operations. Public access is intentionally limited to registration, login, and a small
 * operational/documentation surface; every other endpoint requires an authenticated token.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "application.security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    /**
     * Builds the shared HMAC secret key used for token signing and validation.
     *
     * @param properties configured JWT settings
     * @return secret key for HS256 operations
     */
    @Bean
    public SecretKey jwtSecretKey(JwtSecurityProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Exposes the JWT encoder used when issuing access tokens after successful login.
     *
     * @param jwtSecretKey shared HMAC secret
     * @return encoder backed by the configured symmetric key
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Exposes the JWT decoder used for bearer-token authentication.
     *
     * @param jwtSecretKey shared HMAC secret
     * @param properties configured JWT settings
     * @return JWT decoder with issuer validation applied when configured
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(tokenValidator(properties.issuer()));
        return decoder;
    }

    /**
     * Builds the main servlet security filter chain.
     *
     * @param http servlet security builder
     * @param jwtDecoder component used to validate incoming tokens
     * @param jwtAuthenticationConverter maps token claims to Spring authorities
     * @param authenticationEntryPoint renders JSON for unauthenticated failures
     * @param accessDeniedHandler renders JSON for authorization failures
     * @return configured filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/customers").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    /**
     * Converts custom role and permission claims into Spring authorities.
     *
     * @return converter used by the resource server support
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractAuthorities);
        return converter;
    }

    /** @return JSON authentication entry point used for 401 responses */
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /** @return JSON access denied handler used for 403 responses */
    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }

    /**
     * Builds the token validator with optional issuer verification.
     *
     * @param issuer expected issuer value
     * @return validator used by the JWT decoder
     */
    static OAuth2TokenValidator<@NotNull Jwt> tokenValidator(String issuer) {
        return (issuer == null || issuer.isBlank())
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(issuer);
    }

    /**
     * Extracts roles and fine-grained permissions from the JWT claims.
     *
     * @param jwt validated JWT token
     * @return combined authority collection understood by Spring Security
     */
    static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            authorities.addAll(roles.stream().map(SimpleGrantedAuthority::new).toList());
        }
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            authorities.addAll(permissions.stream().map(SimpleGrantedAuthority::new).toList());
        }
        return authorities;
    }
}
