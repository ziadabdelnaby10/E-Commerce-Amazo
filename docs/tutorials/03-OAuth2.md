# OAuth2: Delegated Authentication and Authorization

> Learn OAuth2 flows for delegated authentication, third-party integrations, and secure API authorization without managing passwords.

## 📋 Concepts

### OAuth2 vs JWT

**JWT**: Self-contained token, user authenticates directly
```
User → User Service → Issues JWT → User uses JWT for all services
```

**OAuth2**: Delegated authentication, third-party provider handles login
```
User → Google/GitHub → Grants permission → Third-party app gets access
```

### OAuth2 Flows

#### 1. Authorization Code Flow (Most Common)

```
1. User clicks "Login with Google"
   ↓
2. Browser redirected to: https://accounts.google.com/auth?client_id=xxx&redirect_uri=http://localhost:8001/login/oauth2/code/google
   ↓
3. Google prompts user to login and grant permissions
   ↓
4. User approves
   ↓
5. Google redirects back to: http://localhost:8001/login/oauth2/code/google?code=AUTH_CODE_FROM_GOOGLE
   ↓
6. Backend exchanges AUTH_CODE for access_token (server-to-server, secure)
   ↓
7. Backend creates session/JWT with user info
   ↓
8. User logged in ✓
```

**Code Example:**
```
GET /oauth2/authorization/google
    ↓
User consents on Google's server
    ↓
POST /login/oauth2/code/google?code=xyz  (from Spring Security)
    ↓
Exchange code for token (backend)
    ↓
Fetch user info from Google
    ↓
Create local user, issue JWT
```

#### 2. Client Credentials Flow (Service-to-Service)

```
Service A needs to call Service B API
    ↓
POST /oauth2/token
{
  "grant_type": "client_credentials",
  "client_id": "service-a",
  "client_secret": "secret-xyz"
}
    ↓
OAuth2 Server issues access_token
    ↓
Service A uses token to call Service B
    GET /api/data
    Authorization: Bearer <token>
    ↓
Service B validates token, processes request
```

**Use Case in Your Project**: Order Service → Inventory Service

### OAuth2 Components

| Component | Role |
|-----------|------|
| **Resource Owner** | User who owns the data |
| **Resource Server** | API holding user data (Google, GitHub) |
| **Authorization Server** | Validates credentials, issues tokens |
| **Client** | Your application requesting access |

## 🏗️ Implementation in Your Project

### Option 1: Using Google OAuth2 for User Service

```java
// Dependency
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-client</artifactId>
</dependency>

// application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: http://localhost:8001/login/oauth2/code/google
            scope: openid,profile,email
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://www.googleapis.com/oauth2/v4/token
            user-info-uri: https://www.googleapis.com/oauth2/v1/userinfo
            user-info-authentication-method: header
            user-name-attribute: email
```

```java
// Security Config
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private OAuth2UserService customUserService;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2Login()
                .userInfoEndpoint()
                    .userService(customUserService)  // Custom loading
            .and()
            .logout()
                .logoutSuccessUrl("/");
    }
}

// Custom User Service: Load OAuth2 user info
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // Extract user info from OAuth2 provider
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        
        // Find or create user in our database
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPicture(picture);
                newUser.setRole(UserRole.CUSTOMER);
                return userRepository.save(newUser);
            });
        
        log.info("OAuth2 user logged in: {}", email);
        
        return new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            oAuth2User.getAttributes(),
            "email"
        );
    }
}

// Controller: Get JWT after OAuth2 login
@RestController
public class OAuth2Controller {
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        
        // Issue JWT (since OAuth2 doesn't directly give us JWT)
        String jwt = jwtProvider.generateAccessToken(new OAuth2UserDetails(email));
        
        return ResponseEntity.ok(new LoginResponse(jwt, email));
    }
}
```

### Option 2: Service-to-Service OAuth2 (Order → Inventory)

```java
// Inventory Service: Authorization Server
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
    
    @Override  
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
            .withClient("order-service")
            .secret(passwordEncoder().encode("order-service-secret"))
            .scopes("inventory:read", "inventory:write")
            .authorizedGrantTypes("client_credentials");
    }
    
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.tokenKeyAccess("permitAll()")
                .checkTokenAccess("permitAll()");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// Order Service: OAuth2 Client
@Configuration
public class OAuth2ClientConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

@Service
@Slf4j
public class InventoryServiceClient {
    
    @Autowired
    RestTemplate restTemplate;
    
    @Value("${inventory-service.url:http://localhost:8003}")
    String inventoryServiceUrl;
    
    @Value("${inventory-service.client-id}")
    String clientId;
    
    @Value("${inventory-service.client-secret}")
    String clientSecret;
    
    private String cachedAccessToken;
    private long tokenExpiryTime;
    
    /**
     * Get access token from Authorization Server
     */
    private String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedAccessToken;  // Use cached token if not expired
        }
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            inventoryServiceUrl + "/oauth/token",
            new HttpEntity<>(body, new HttpHeaders()),
            Map.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            cachedAccessToken = (String) response.getBody().get("access_token");
            Long expiresIn = ((Number) response.getBody().get("expires_in")).longValue();
            tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);
            
            log.info("Obtained access token from inventory service");
            return cachedAccessToken;
        }
        
        throw new RuntimeException("Failed to obtain access token");
    }
    
    /**
     * Call Inventory Service with OAuth2 token
     */
    public Product getProductStock(String productId) {
        String token = getAccessToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        ResponseEntity<Product> response = restTemplate.exchange(
            inventoryServiceUrl + "/products/" + productId + "/stock",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Product.class
        );
        
        return response.getBody();
    }
}
```

## ⚙️ When to Use OAuth2

**Use OAuth2 when:**
- ✅ Third-party login (Google, GitHub, Social)
- ✅ Delegating authentication to specialized provider
- ✅ User doesn't want to share password
- ✅ Service-to-service communication with revocation needs
- ✅ Building authorization servers

**Don't use OAuth2 for:**
- ❌ Simple internal authentication (JWT simpler)
- ❌ Lightweight APIs (added complexity)
- ❌ Offline-first systems

## 🔴 Common Pitfalls

### 1. Mixing Up Client Secret Locations

❌ **Problem**: Exposing client_secret in frontend
```javascript
// Frontend JavaScript
fetch('/oauth/token', {
  client_id: "xxx",
  client_secret: "secret_should_not_be_here"  // Visible to user!
})
```

✅ **Solution: Backend handles OAuth2**
```
Frontend → Backend (secure)
Backend → OAuth Provider (using hidden secret)
Backend → Frontend (with JWT)
```

### 2. Not Verifying State Parameter

❌ **Problem**: CSRF vulnerability
```
Attacker tricks user into clicking:
http://localhost:8001/login/oauth2/code/google?code=ATTACKER_CODE
```

✅ **Solution: Spring Security handles this automatically**
```yaml
# Already included in spring-security-oauth2-client
# Verifies state parameter matches
```

### 3. Storing Refresh Token Insecurely

❌ **Problem**: Refresh token in localStorage
```javascript
localStorage.setItem('refreshToken', token);  // Vulnerable to XSS!
```

✅ **Solution: HTTP-only secure cookies**
```java
Cookie cookie = new Cookie("refreshToken", refreshToken);
cookie.setHttpOnly(true);
cookie.setSecure(true);
cookie.setPath("/");
response.addCookie(cookie);
```

## 🔗 Resources

- [OAuth 2.0 Specification](https://tools.ietf.org/html/rfc6749)
- [Spring Security OAuth2 Docs](https://spring.io/projects/spring-security-oauth2)
- [Understanding OAuth2](https://www.oauth.com/)
- Your Project: Planned integration with Google login

---

**Next**: Read [JWT](06-JWT.md) for token structure, or [API Gateway](05-API-Gateway.md) for centralized authentication.

