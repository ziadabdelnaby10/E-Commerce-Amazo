# Authorization Quick Reference

## For Developers

### Adding Authorization to New Endpoints

If you need to add authorization to a new endpoint, follow these steps:

#### 1. Add @PreAuthorize to Method
```java
@PostMapping("/orders")
@PreAuthorize("hasAuthority('CREATE_ORDER')")
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
    // implementation
}
```

#### 2. Get Required Authority
Authority values are defined by user-service and include:
- `CREATE_ORDER` - Create orders
- `VIEW_ORDERS` - View orders
- `PROCESS_PAYMENT` - Process payments, reserve inventory
- `VIEW_PAYMENTS` - View payments
- `MODIFY_INVENTORY` - Modify products and stock
- `VIEW_INVENTORY` - View products and stock
- `VIEW_USERS` - View user data, notifications

#### 3. User-Specific Access
For user-owned resources, use SpEL expressions:
```java
@PreAuthorize("authentication.principal.claims['userId'] == #userId.toString() or hasAnyAuthority('ADMIN')")
public ResponseEntity<UserData> getUserData(@PathVariable Long userId) {
    // implementation
}
```

### Testing Authorization

#### Mock JWT in Tests
```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean JwtDecoder jwtDecoder;
    
    @Test
    void testCreateOrderWithToken() throws Exception {
        // Mock valid JWT
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("authorities", Arrays.asList("CREATE_ORDER"))
            .build();
        
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        
        mockMvc.perform(post("/v1/orders")
            .header("Authorization", "Bearer token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated());
    }
}
```

### Calling Other Services

When service A calls service B:

```java
@Service
public class OrderService {
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private HttpServletRequest httpRequest;
    
    public void createOrder(Order order) {
        // Get JWT from current request
        String jwt = httpRequest.getHeader("Authorization");
        
        // Include in downstream call
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwt);
        
        restTemplate.exchange(
            "http://inventory-service/v1/inventory/reservations",
            HttpMethod.POST,
            new HttpEntity<>(reserveRequest, headers),
            ReserveResponse.class
        );
    }
}
```

## Common Questions

### Q: How do I get the current user's ID?
```java
@GetMapping
public ResponseEntity<UserOrders> getMyOrders() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) auth.getPrincipal();
    Long userId = jwt.getClaimAsNumber("userId").longValue();
    // use userId
}
```

### Q: How do I check if user has an authority?
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
boolean hasPermission = auth.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("CREATE_ORDER"));
```

### Q: What if authorization check fails?
- 401 Unauthorized: No valid JWT provided
- 403 Forbidden: Valid JWT but user lacks required authority
- 400 Bad Request: Invalid JWT format

### Q: How to disable authorization for development?
Set in application.yml:
```yaml
application:
  security:
    enabled: false
```

### Q: How to change the JWT secret?
Update in config-service YAML:
```yaml
application:
  security:
    jwt:
      secret: new-secret-key-here
```
All services will pick up the change automatically.

## Production Checklist

- [ ] JWT secret is complex and random
- [ ] JWT secret is stored securely (not in code)
- [ ] Token expiration is set (15m recommended)
- [ ] HTTPS is enforced
- [ ] CORS is properly configured
- [ ] Audit logging is enabled
- [ ] Token validation errors are logged
- [ ] All endpoints are properly annotated
- [ ] Health checks bypass authentication
- [ ] Database queries are parameterized
- [ ] Sensitive data is not logged

## Monitoring

### Logs to Watch
```
# Successful authorization
log: "Authorization successful for user {userId} on endpoint {endpoint}"

# Failed authorization
log: "Authorization failed for user {userId}: missing authority {authority}"

# Invalid tokens
log: "Failed to decode JWT: {error}"
```

### Metrics to Track
- Authorization success rate
- Authorization failure rate by endpoint
- Token validation time
- Failed authority checks

## Emergency Procedures

### Revoke All Tokens
To force re-authentication of all users:
1. Change JWT secret in config-service
2. Restart all downstream services
3. All existing tokens become invalid

### Disable Authorization Temporarily
Set in config-service:
```yaml
application:
  security:
    enabled: false
```

### Extend Token Expiration
Update in config-service:
```yaml
application:
  security:
    jwt:
      access-token-ttl: 30m  # Increased from 15m
```

## References

- Spring Security Docs: https://docs.spring.io/spring-security/reference/
- JWT RFC: https://tools.ietf.org/html/rfc7519
- SpEL Documentation: https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions
- OWASP Security Guidelines: https://owasp.org/www-project-top-ten/

