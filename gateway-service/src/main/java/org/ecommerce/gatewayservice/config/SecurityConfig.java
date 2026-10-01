package org.ecommerce.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reactive Spring Security configuration for the gateway.
 *
 * <p>The gateway validates bearer tokens before forwarding protected traffic to downstream
 * services. Although downstream services still validate JWTs themselves, gateway-level
 * validation gives early rejection, consistent error bodies, and a central place to enforce
 * route-level authorization rules.</p>
 *
 * <p>This configuration is only activated when {@code application.security.enabled=true},
 * which allows simpler local troubleshooting if security ever needs to be turned off.</p>
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(prefix = "application.security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

    /**
     * Builds the HMAC secret key used to verify HS256 access tokens.
     *
     * <p>The same shared secret must be used by the token issuer, which in this project
     * is the authentication flow in {@code customer-service}.</p>
     *
     * @param properties externalized JWT settings
     * @return secret key for JWT signature validation
     */
    @Bean
    public SecretKey jwtSecretKey(JwtSecurityProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Configures the reactive JWT decoder used by the resource server support.
     *
     * <p>The decoder validates both the token signature and, when configured, the expected
     * issuer claim. If no issuer is configured the default validators are still applied.</p>
     *
     * @param jwtSecretKey shared HMAC key
     * @param properties externalized JWT settings
     * @return configured reactive JWT decoder
     */
    @Bean
    public NimbusReactiveJwtDecoder reactiveJwtDecoder(SecretKey jwtSecretKey, JwtSecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        String issuer = properties.issuer();
        decoder.setJwtValidator((issuer == null || issuer.isBlank())
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /**
     * Builds the reactive security filter chain for the gateway.
     *
     * <p>Public endpoints are intentionally limited to documentation endpoints, health probes,
     * auth endpoints, customer registration, and the public inventory read endpoint. All other
     * routes require a valid authenticated JWT.</p>
     *
     * <p>Custom handlers are plugged in so authentication and authorization failures produce
     * consistent JSON responses instead of the framework defaults.</p>
     *
     * @param http reactive HTTP security builder
     * @param reactiveJwtDecoder JWT decoder for token verification
     * @param jwtAuthenticationConverter maps token claims into Spring authorities
     * @param objectMapper serializer used by custom error handlers
     * @return configured security chain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            NimbusReactiveJwtDecoder reactiveJwtDecoder,
                                                            ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter
            , ObjectMapper objectMapper) {
        JsonServerAuthenticationEntryPoint authenticationEntryPoint = new JsonServerAuthenticationEntryPoint(objectMapper);
        JsonServerAccessDeniedHandler accessDeniedHandler = new JsonServerAccessDeniedHandler(objectMapper);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Preflight requests must remain open for browser clients.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Operational and documentation endpoints should remain accessible.
                        .pathMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**", "/fallback/**").permitAll()
                        // Authentication endpoints are public by design.
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // Public product browsing is allowed without login.
                        .pathMatchers(HttpMethod.GET, "/api/v1/inventory/product").permitAll()
                        // New customer registration is public.
                        .pathMatchers(HttpMethod.POST, "/api/v1/customers").permitAll()
                        // Every remaining route requires a valid authenticated principal.
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    /**
     * Converts custom JWT claims into Spring Security authorities.
     *
     * <p>The token contains both role-style and permission-style claims. Both are promoted into
     * {@link GrantedAuthority} objects so route and method security can use a unified model.</p>
     *
     * @return reactive adapter that exposes token claims as authorities
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // Roles are added exactly as issued in the token.
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                authorities.addAll(roles.stream().map(SimpleGrantedAuthority::new).toList());
            }
            // Fine-grained permissions are exposed alongside roles.
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            if (permissions != null) {
                authorities.addAll(permissions.stream().map(SimpleGrantedAuthority::new).toList());
            }
            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}




