package org.ecommerce.customerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security-related cryptographic bean configuration.
 *
 * <p>The customer-service stores password hashes, never raw passwords. BCrypt is used
 * here because it is a battle-tested adaptive hashing algorithm designed for password
 * storage rather than for general-purpose encryption.</p>
 */
@Configuration
public class CryptoConfig {

    /**
     * Exposes the password encoder used by registration and login flows.
     *
     * @return BCrypt password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

