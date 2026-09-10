# Authorization Implementation Across Downstream Services

## Overview

This document describes the implementation of JWT-based authorization across the downstream services (order-service, payment-service, notification-service, and inventory-service) in the E-Commerce microservices platform.

## Architecture

### Security Model

- **Authentication**: JWT tokens issued by user-service
- **Authorization**: Spring Security @PreAuthorize method-level authorization
- **Token Validation**: Stateless JWT decoding using HS256 algorithm
- **Configuration**: Externalized in config-service for centralized management

### Services Affected

1. **order-service** (port 9003)
   - Validates JWT on all order operations
   - Requires `CREATE_ORDER` authority for order creation
   - Requires `VIEW_ORDERS` authority for retrieval operations

2. **payment-service** (port 9004)
   - Validates JWT on all payment operations
   - Requires `PROCESS_PAYMENT` authority for payment initiation
   - Requires `VIEW_PAYMENTS` authority for retrieval operations

3. **notification-service** (port 9005)
   - Validates JWT on all notification operations
   - User-specific inbox access with custom authorization logic
   - Preference endpoints require user-owned resource validation

4. **inventory-service** (port 9002)
   - Validates JWT for protected endpoints
   - Requires `MODIFY_INVENTORY` for write operations
   - Requires `VIEW_INVENTORY` for read operations
   - Special handling for inter-service inventory reservation calls

## Implementation Details

### Common Security Configuration

Each service implements a `SecurityConfig` class that:

1. Extracts JWT secret from application properties
2. Configures JwtDecoder with HS256 algorithm
3. Sets up stateless session management
4. Allows health/actuator and Swagger endpoints without authentication
5. Requires valid JWT for all other endpoints

**Key Configuration Points:**
```yaml
application:
  security:
    enabled: true
    jwt:
      secret: I_gu6p0FVNHP_Hhjt0cHXF-eFwYGB8ph1xwUOKaBYljDfsXnHpfYAYgaRC8msRTc
      issuer: ecommerce-platform
      access-token-ttl: 15m
```

### Endpoint Authorization

#### Order Service
- `POST /v1/orders` → `hasAuthority('CREATE_ORDER')`
- `GET /v1/orders/{orderId}` → `hasAuthority('VIEW_ORDERS')`
- `GET /v1/orders` → `hasAuthority('VIEW_ORDERS')`

#### Payment Service
- `POST /v1/payments` → `hasAuthority('PROCESS_PAYMENT')`
- `GET /v1/payments/{paymentId}` → `hasAuthority('VIEW_PAYMENTS')`
- `GET /v1/payments` → `hasAuthority('VIEW_PAYMENTS')`

#### Notification Service
- `GET /v1/notifications` → Custom SpEL expression for user-specific access
- `GET /v1/notifications/{notificationId}` → `hasAuthority('VIEW_USERS')`
- `GET /v1/notifications/preferences/{userId}` → Custom user-owned validation
- `PUT /v1/notifications/preferences/{userId}` → Custom user-owned validation

#### Inventory Service
- `POST /v1/inventory/product` → `hasAuthority('MODIFY_INVENTORY')`
- `GET /v1/inventory/product` → `hasAuthority('VIEW_INVENTORY')`
- `POST /v1/inventory/reservations` → `hasAuthority('PROCESS_PAYMENT') or hasAuthority('CREATE_ORDER')`
- `POST /v1/inventory/reservations/release` → `hasAuthority('PROCESS_PAYMENT') or hasAuthority('CREATE_ORDER')`

### Excluded Endpoints

The following endpoints bypass JWT authentication:

- `/actuator/health` - Service health checks
- `/actuator/info` - Service information
- `/v3/api-docs/**` - OpenAPI documentation
- `/swagger-ui/**` - Swagger UI resources
- `/swagger-ui.html` - Swagger UI home
- `/webjars/**` - Swagger UI web assets
- Preflight OPTIONS requests

## Authority Model

| Authority | Issued By | Services | Usage |
|-----------|-----------|----------|-------|
| `CREATE_ORDER` | user-service | order-service | Order creation |
| `VIEW_ORDERS` | user-service | order-service | Order retrieval |
| `PROCESS_PAYMENT` | user-service | payment-service, inventory-service | Payment operations, inventory reservation |
| `VIEW_PAYMENTS` | user-service | payment-service | Payment retrieval |
| `MODIFY_INVENTORY` | user-service | inventory-service | Product/stock management |
| `VIEW_INVENTORY` | user-service | inventory-service | Product/stock retrieval |
| `VIEW_USERS` | user-service | notification-service | Notification management |

## JWT Token Structure

Expected JWT claims:
```json
{
  "sub": "user123",
  "userId": 123,
  "email": "user@example.com",
  "authorities": ["CREATE_ORDER", "VIEW_ORDERS", "PROCESS_PAYMENT"],
  "iss": "ecommerce-platform",
  "exp": 1234567890,
  "iat": 1234567800
}
```

## Inter-Service Communication

### Service-to-Service Calls

When services call each other (e.g., order-service calls inventory-service):

1. The calling service includes the original JWT in the Authorization header
2. The receiving service validates the JWT
3. Authorization checks are performed on behalf of the original user
4. Service account permissions can be added in future for service-to-service operations

**Example:**
```
1. Client (with JWT) → order-service POST /v1/orders
2. order-service validates JWT and checks CREATE_ORDER authority
3. order-service → inventory-service POST /v1/inventory/reservations (includes JWT)
4. inventory-service validates JWT and checks PROCESS_PAYMENT or CREATE_ORDER
```

## Configuration in Config Service

Each service's configuration is managed through config-service YAML files:

- `config-service/src/main/resources/config/order-service.yml`
- `config-service/src/main/resources/config/payment-service.yml`
- `config-service/src/main/resources/config/notification-service.yml`
- `config-service/src/main/resources/config/inventory-service.yml` (if applicable)

The JWT secret is centrally managed and can be rotated by updating the config-service without redeploying downstream services.

## Testing Authorization

### Unit Tests

Each service includes tests for authorization using:

```java
@WebMvcTest(OrderController.class)
class OrderControllerAuthorizationTest {
    @MockBean
    private JwtDecoder jwtDecoder;
    
    @Test
    void testCreateOrderWithoutAuthorization_ShouldReturn401() {
        // Test without JWT
    }
    
    @Test
    void testCreateOrderWithInsufficientPermissions_ShouldReturn403() {
        // Test with valid JWT but wrong authorities
    }
    
    @Test
    void testCreateOrderWithRequiredAuthority_ShouldSucceed() {
        // Test with valid JWT and required authority
    }
}
```

### Integration Tests

Integration tests verify JWT validation with real tokens issued by user-service.

## Migration Path

### Phase 1 (Current)
- Implement SecurityConfig in downstream services
- Add @PreAuthorize annotations to endpoints
- Enable authorization in application.yml

### Phase 2
- Update API Gateway to validate and forward JWT
- Implement service-to-service JWT validation
- Add audit logging of authorization decisions

### Phase 3
- Implement role-based access control (RBAC)
- Add fine-grained permissions
- Implement dynamic authority loading

## Troubleshooting

### Common Issues

1. **401 Unauthorized on authenticated requests**
   - Verify JWT token hasn't expired
   - Check JWT secret in config-service matches user-service
   - Ensure Authorization header format: `Bearer <token>`

2. **403 Forbidden with valid JWT**
   - Verify user has required authority in token
   - Check @PreAuthorize expression syntax
   - Review role mappings in user-service

3. **Security filters not active**
   - Verify `application.security.enabled: true` in YAML
   - Check SecurityConfig class is being scanned by ComponentScan
   - Ensure dependencies are correctly specified in pom.xml

## Security Considerations

1. **Secret Management**
   - JWT secret should be at least 256 bits
   - Rotate secrets periodically
   - Use environment variables in production

2. **Token Handling**
   - Never log token contents
   - Implement token expiration (15 minutes default)
   - Add refresh token mechanism for user-service

3. **CORS Configuration**
   - Configure CORS only for trusted origins
   - Avoid CORS on SecurityFilterChain if possible
   - Handle preflight requests appropriately

## References

- Spring Security OAuth2 Resource Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html
- JWT Best Practices: https://tools.ietf.org/html/rfc8725
- Spring Security Method-level Security: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html

