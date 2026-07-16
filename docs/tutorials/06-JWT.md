# JWT: JSON Web Tokens for Stateless Authentication

> Master JWT tokens, token claims, signature verification, and stateless authentication patterns for securing your microservices.

## 📋 Concepts

### The Authentication Problem

**Traditional: Session-Based (Monolith)**
```
Login:
  User submits credentials
  ↓
  Server validates, creates session in memory/database
  ↓
  Returns session_id in cookie
  
Each Request:
  Browser sends cookie automatically
  ↓
  Server looks up session
  ↓
  Verifies user identity
  ↓
  Processes request
  
Problem in Microservices: 
  Which service stores sessions? 
  → Need shared session store (defeats autonomy)
```

**JWT: Token-Based (Microservices)**
```
Login:
  User submits credentials
  ↓
  Server creates signed token
  ↓
  Token: {userId, role, expiry, signature}
  
Each Request:
  Client sends Authorization: Bearer <token> header
  ↓
  Service verifies signature (offline, no DB lookup)
  ↓
  Service trusts token if valid
  ↓
  Processes request
  
Benefit:
  No session storage needed
  Stateless: each service can verify independently
  Scalable: works with microservices
```

### JWT Structure

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

├─────────────────────────────────── Header ────────────────────────────────
{
  "alg": "HS256",  // Algorithm (HMAC SHA-256)
  "typ": "JWT"     // Type
}

├─────────────────────────────────── Payload ────────────────────────────────
{
  "sub": "user-123",          // Subject (user ID)
  "name": "John Doe",
  "role": "CUSTOMER",
  "iat": 1516239022,          // Issued at (timestamp)
  "exp": 1516325422,          // Expiration
  "iss": "order-service"      // Issuer
}

├─────────────────────────────────── Signature ────────────────────────────────
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  "your-secret-key"
)
```

**Key Points:**
- Header + Payload is **NOT encrypted** (base64 = reversible)
- Signature proves the token wasn't tampered with
- Token is **tamper-proof** but not private (don't put secrets)
- Server uses the same secret key to verify signature

### Token Lifecycle

```
1. User logs in with credentials
   POST /auth/login { email, password }
   ↓
2. Server validates, generates JWT
   ├─ Access Token: Short-lived (15 min) - for API calls
   └─ Refresh Token: Long-lived (7 days) - to get new access token
   ↓
3. Client receives tokens, stores them
   └─ Access Token: In-memory or localStorage
   └─ Refresh Token: Secure HTTP-only cookie or localStorage
   ↓
4. Client makes requests with Access Token
   GET /orders
   Authorization: Bearer eyJhbGc...
   ↓
5. Server validates Access Token signature
   ├─ Valid? Process request ✓
   ├─ Expired? Return 401
   └─ Invalid? Return 403

6. Access Token expires → Client uses Refresh Token
   POST /auth/refresh
   { refreshToken: "eyJhbGc..." }
   ↓
7. Server validates Refresh Token, issues new Access Token
   └─ Return new Access Token (15 min valid)
```

## 🏗️ Implementation in Your Project

### User Service: JWT Generation & Validation

```java
// Config: Setup JWT properties
@Configuration
@Slf4j
public class JwtConfig {
    
    @Value("${jwt.secret:your-super-secret-key-change-in-production}")
    private String secret;
    
    @Value("${jwt.expiration:900000}")  // 15 minutes
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")  // 7 days
    private long refreshTokenExpiration;
    
    @Bean
    public JwtProvider jwtProvider() {
        return new JwtProvider(secret, accessTokenExpiration, refreshTokenExpiration);
    }
}

// Provider: Generates and validates tokens
@Component
@Slf4j
public class JwtProvider {
    private final String secret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    
    public JwtProvider(String secret, long accessTokenExpiration, long refreshTokenExpiration) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
    
    /**
     * Generate access token (short-lived)
     */
    public String generateAccessToken(UserDetails user) {
        return Jwts.builder()
            .setSubject(user.getUsername())  // User ID
            .claim("roles", user.getAuthorities())
            .claim("tokenType", "ACCESS")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    /**
     * Generate refresh token (long-lived)
     */
    public String generateRefreshToken(UserDetails user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("tokenType", "REFRESH")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secret)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    /**
     * Validate token signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Jwt Exception e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract claims (user data) from token
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secret)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}

// Controller: Registration & Login
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Authenticate user (validates password)
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            UserDetails user = (UserDetails) auth.getPrincipal();
            
            // Generate tokens
            String accessToken = jwtProvider.generateAccessToken(user);
            String refreshToken = jwtProvider.generateRefreshToken(user);
            
            log.info("User logged in: {}", request.getEmail());
            
            return ResponseEntity.ok(new LoginResponse(
                accessToken,
                refreshToken,
                900  // Access token expires in 15 minutes
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        if (!jwtProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
        
        String userId = jwtProvider.extractUserId(request.getRefreshToken());
        UserDetails user = userService.loadUserByUsername(userId);
        String newAccessToken = jwtProvider.generateAccessToken(user);
        
        return ResponseEntity.ok(new LoginResponse(
            newAccessToken,
            request.getRefreshToken(),
            900
        ));
    }
}
```

### Validation Filter: Intercept All Requests

```java
// Filter: Validates JWT on every request
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) extends IOException, ServletException {
        
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        // Extract token from "Bearer <token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token != null) {
            // Validate token
            if (jwtProvider.validateToken(token)) {
                Claims claims = jwtProvider.extractClaims(token);
                String userId = claims.getSubject();
                
                // Create authentication object
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                List<SimpleGrantedAuthority> authorities = 
                    roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
                // Set in SecurityContext (makes it available in controllers)
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                log.debug("JWT validated for user: {}", userId);
            } else {
                log.warn("Invalid JWT token");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

// Security Config: Register filter
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/auth/login", "/auth/refresh").permitAll()
                .antMatchers("/orders/**").authenticated()
                .antMatchers("/admin/**").hasAuthority("ADMIN")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
```

### Using JWT in Controllers

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @GetMapping
    public ResponseEntity<?> getOrders(@RequestHeader("Authorization") String token) {
        // Extract user from token
        String userId = jwtProvider.extractUserId(token.substring(7));
        
        List<Order> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestBody OrderRequest request,
        @AuthenticationPrincipal UserDetails user) {  // Automatically injected
        
        Order order = orderService.createOrder(user.getUsername(), request);
        return ResponseEntity.status(201).body(order);
    }
}
```

## ⚙️ When to Use JWT

**Use JWT when:**
- ✅ Stateless authentication needed
- ✅ Microservices need independent verification
- ✅ Mobile apps (no cookie storage)
- ✅ APIs consumed by third parties
- ✅ Cross-domain requests (CORS-friendly)

**Don't use JWT for:**
- ❌ Immediately revoking access (JW doesn't support it easily)
- ❌ Real-time permission changes
- ❌ Highly sensitive data (tokens are visible)

## 🔴 Common Pitfalls

### 1. Storing Secrets Insecurely

❌ **Problem**: Secret key in code
```java
.signWith(SignatureAlgorithm.HS512, "my-secret-key")  // Hardcoded!
```

✅ **Solution**: Use environment variables**
```yaml
jwt:
  secret: ${JWT_SECRET:default-change-in-prod}
```

### 2. Token Never Expires

❌ **Problem**: Access token valid forever
```java
.setExpiration(new Date(Long.MAX_VALUE))  // Never!
```

✅ **Solution: Short-lived access tokens**
```java
.setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))  // 15 min
```

### 3. Storing Sensitive Data in Token

❌ **Problem**: Putting password in token
```java
.claim("password", user.getPassword())  // Anyone can decode!
```

✅ **Solution: Only store non-sensitive data**
```java
.claim("userId", user.getId())
.claim("role", user.getRole())  // OK
```

## 🔗 Resources

- [JWT.io - Debugger & Documentation](https://jwt.io/)
- [JJWT Library](https://github.com/jwtk/jjwt)
- [Spring Security JWT Guide](https://spring.io/blog/2015/01/12/the-login-page-filter)
- Your Project: User Service implements JWT authentication

---

**Next**: Read [OAuth2](03-OAuth2.md) for delegated authentication, or [API Gateway](05-API-Gateway.md) for centralized JWT validation.

