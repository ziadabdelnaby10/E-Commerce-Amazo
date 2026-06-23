# ADR-003: Security Architecture

**Date:** 2026-06-22  
**Status:** Accepted  
**References:** ADR-001

## Problem

How should we implement authentication, authorization, and data protection across microservices?

## Decision

Implement **JWT-Based Authentication with Role-Based Access Control**:
- User Service generates JWT tokens
- All services validate JWT (Spring Security filters)
- Redis stores refresh tokens and user sessions
- Payment Service encrypts sensitive data
- Environment-based secrets management

## Authentication Flow

### User Registration
```
POST /auth/register
{
  "email": "user@example.com",
  "password": "securePassword",
  "firstName": "John",
  "lastName": "Doe"
}
↓
1. User Service: Validate input
2. User Service: Hash password (BCrypt, rounds=12)
3. User Service: Store in DB
4. Response: {"userId": "123", "email": "..."}
```

### User Login
```
POST /auth/login
{
  "email": "user@example.com",
  "password": "securePassword"
}
↓
1. User Service: Find user by email
2. User Service: Verify password (BCrypt)
3. User Service: Generate access token (JWT, 15 min)
4. User Service: Generate refresh token
5. Redis: Store refresh token (TTL: 7 days)
6. Response: {
     "accessToken": "eyJhbGc...",
     "refreshToken": "xyz...",
     "expiresIn": 900,
     "user": {...}
   }
```

### Token Refresh
```
POST /auth/refresh
{
  "refreshToken": "xyz..."
}
↓
1. User Service: Validate refresh token in Redis
2. User Service: Generate new access token
3. Response: {"accessToken": "...", "expiresIn": 900}

Token Validation (All Services)
↓
1. Extract JWT from Authorization header
2. Spring Security filter validates signature
3. Check token expiry
4. Extract claims (userId, roles)
5. Load user permissions
```

## JWT Token Structure

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Claims):
{
  "sub": "user-123",              // Subject (userId)
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1623456000,              // Issued at
  "exp": 1623456900,              // Expiration (15 min)
  "iss": "user-service",          // Issuer
  "aud": "order-service"          // Audience
}

Signature: HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret_key
)
```

## Authorization Model (RBAC)

### Roles
```
ROLE_ADMIN
  - Can view all orders
  - Can cancel orders
  - Can refund payments
  - Can modify inventory

ROLE_USER
  - Can create orders
  - Can view own orders
  - Can cancel own orders

ROLE_SUPPORT
  - Can view all orders
  - Can view payments
  - Cannot modify orders

ROLE_SYSTEM
  (Internal service-to-service calls)
```

### Access Control Rules

```java
@GetMapping("/orders/{id}")
@PreAuthorize("hasRole('USER') and @orderService.isOwner(#id, principal)")
public OrderDTO getOrder(@PathVariable Long id) { ... }

@DeleteMapping("/orders/{id}")
@PreAuthorize("hasRole('USER') and @orderService.isOwner(#id, principal) or hasRole('ADMIN')")
public void cancelOrder(@PathVariable Long id) { ... }
```

## Service-to-Service Authentication

### Option 1: Service Account Token (Synchronous)
```
Order Service → Inventory Service

1. Order Service creates JWT claim:
   {"sub": "order-service", "roles": ["ROLE_SYSTEM"]}
2. Include token in Authorization header
3. Inventory Service validates token signature
4. Check if "ROLE_SYSTEM" role present
```

### Option 2: API Key (Alternative)
```
Header: "X-API-Key: order-service-key-xyz"
Inventory Service looks up key in Redis
```

## Data Encryption (Payment Service)

### At-Rest Encryption
```java
@Service
public class PaymentEncryptionService {
    private final StringEncryptor encryptor;
    
    public String encryptCardNumber(String cardNumber) {
        return encryptor.encrypt(cardNumber);
    }
    
    public String decryptCardNumber(String encrypted) {
        return encryptor.decrypt(encrypted);
    }
}

// In entity:
@Entity
public class Payment {
    @Convert(converter = StringEncryptor.class)
    private String cardNumber;  // Encrypted in database
}
```

### In-Transit Encryption
- All inter-service calls over HTTPS (production)
- Local development: HTTP (acceptable)
- Kafka messages: Consider encryption for PII

## Secrets Management

### Local Development (docker-compose)
```yaml
environment:
  JWT_SECRET: ${JWT_SECRET:-local-dev-secret-change-in-prod}
  ENCRYPTION_KEY: ${ENCRYPTION_KEY:-local-dev-key}
  DB_PASSWORD: ${DB_PASSWORD:-rootroot}
```

### Production (Kubernetes Secrets)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  JWT_SECRET: prod-secret-from-vault
  ENCRYPTION_KEY: prod-key-from-vault
```

## Rate Limiting (API Gateway)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8002
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100      # 100 requests
                  burstCapacity: 200      # Can spike to 200
                  requestedTokens: 1
```

## Audit Logging

### Security Events to Log
```
- User login (success/failure)
- User registration
- Token refresh
- Failed authentication attempts
- Unauthorized access attempts
- Payment operations
- Data modifications by admin

Log Format:
{
  "timestamp": "2026-06-22T10:30:00Z",
  "eventType": "USER_LOGIN",
  "userId": "user-123",
  "result": "SUCCESS",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "details": {...}
}
```

## CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .maxAge(3600);
            }
        };
    }
}
```

## SQL Injection Prevention

✅ Always use prepared statements:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Uses prepared statement internally
    User findByEmail(String email);
    
    // For complex queries
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailCustom(@Param("email") String email);
}
```

❌ NEVER use string concatenation:
```java
// WRONG - Vulnerable to SQL injection
String query = "SELECT * FROM users WHERE email = '" + email + "'";
```

## Cross-Site Scripting (XSS) Prevention

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    // Response is automatically JSON-escaped
    @GetMapping("/{id}")
    public OrderDTO getOrder(@PathVariable Long id) {
        // Special characters in JSON are escaped: &, <, >, ", etc.
    }
}
```

## CSRF Protection

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .authorizeRequests()
                .antMatchers("/auth/**").permitAll()
                .anyRequest().authenticated();
        return http.build();
    }
}
```

## Testing Security

```java
@SpringBootTest
public class SecurityTests {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/orders"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    public void testAuthorizedAccess() throws Exception {
        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk());
    }
}
```

## Next Steps

1. Implement JWT generation in User Service
2. Configure Spring Security in User Service
3. Add JWT validation filter to all services
4. Implement role-based access control (@PreAuthorize)
5. Add audit logging
6. Implement encryption in Payment Service
7. Create security integration tests
