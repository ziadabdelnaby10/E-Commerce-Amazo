# Protected Endpoints Reference

This document lists all protected endpoints and their required authorities.

## Order Service (Port 9003)

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `POST` | `/v1/orders` | `CREATE_ORDER` | Create new order |
| `GET` | `/v1/orders/{orderId}` | `VIEW_ORDERS` | Get order by ID |
| `GET` | `/v1/orders` | `VIEW_ORDERS` | List user's orders (paginated) |

**Status Codes:**
- `201` - Order created successfully
- `200` - Successful retrieval
- `400` - Validation error
- `401` - Missing or invalid JWT
- `403` - User lacks required authority

---

## Payment Service (Port 9004)

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `POST` | `/v1/payments` | `PROCESS_PAYMENT` | Initiate payment |
| `GET` | `/v1/payments/{paymentId}` | `VIEW_PAYMENTS` | Get payment by ID |
| `GET` | `/v1/payments` | `VIEW_PAYMENTS` | List user's payments (paginated) |

**Status Codes:**
- `201` - Payment initiated
- `200` - Successful retrieval
- `400` - Validation error
- `401` - Missing or invalid JWT
- `403` - User lacks required authority
- `409` - Payment conflict

---

## Notification Service (Port 9005)

| Method | Endpoint | Required Authority* | Description |
|--------|----------|-------------------|-------------|
| `GET` | `/v1/notifications` | User-owned + `VIEW_USERS` | Get user's notifications |
| `GET` | `/v1/notifications/{notificationId}` | `VIEW_USERS` | Get notification by ID |
| `GET` | `/v1/notifications/preferences/{userId}` | User-owned | Get notification preferences |
| `PUT` | `/v1/notifications/preferences/{userId}` | User-owned | Update notification preferences |

**Status Codes:**
- `200` - Successful retrieval/update
- `204` - No content (preferences updated)
- `400` - Validation error
- `401` - Missing or invalid JWT
- `403` - User lacks required authority
- `404` - Notification/preferences not found

\* **User-Owned Authorization**: Request user ID from JWT must match path parameter OR user must have `ADMIN` authority

---

## Inventory Service (Port 9002)

### Product Management

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `POST` | `/v1/inventory/product` | `MODIFY_INVENTORY` | Create new product |
| `GET` | `/v1/inventory/product` | `VIEW_INVENTORY` | List all products |
| `GET` | `/v1/inventory/product/{productId}` | `VIEW_INVENTORY` | Get product by ID |
| `PUT` | `/v1/inventory/product/{productId}` | `MODIFY_INVENTORY` | Update product |
| `PATCH` | `/v1/inventory/product/{productId}/status` | `MODIFY_INVENTORY` | Update product status |

### Stock Management

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `GET` | `/v1/inventory/{productId}/stock` | `VIEW_INVENTORY` | Get stock level |
| `POST` | `/v1/inventory/{productId}/stock/adjust` | `MODIFY_INVENTORY` | Adjust stock quantity |

### Inventory Reservation (Inter-Service)

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `POST` | `/v1/inventory/reservations` | `PROCESS_PAYMENT` OR `CREATE_ORDER` | Reserve items for order |
| `POST` | `/v1/inventory/reservations/release` | `PROCESS_PAYMENT` OR `CREATE_ORDER` | Release reserved items |

### Transaction History

| Method | Endpoint | Required Authority | Description |
|--------|----------|-------------------|-------------|
| `GET` | `/v1/inventory/{productId}/transactions` | `VIEW_INVENTORY` | Get transaction history |

**Status Codes:**
- `201` - Resource created
- `200` - Successful retrieval
- `204` - No content
- `400` - Validation error
- `401` - Missing or invalid JWT
- `403` - User lacks required authority
- `404` - Resource not found
- `409` - Conflict (e.g., SKU already exists)

---

## Public / Unprotected Endpoints

The following endpoints are accessible without authentication:

### All Services

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Service health check |
| `/actuator/info` | Service information |
| `/v3/api-docs/**` | OpenAPI documentation |
| `/v3/api-docs.json` | OpenAPI JSON |
| `/v3/api-docs.yaml` | OpenAPI YAML |
| `/swagger-ui/**` | Swagger UI resources |
| `/swagger-ui.html` | Swagger UI interface |
| `/webjars/**` | Web assets (Bootstrap, jQuery, etc.) |
| `OPTIONS **` | CORS preflight requests |

---

## Authority Reference

### Authority Definitions

| Authority | Issued By | Services | Permissions |
|-----------|-----------|----------|-------------|
| `CREATE_ORDER` | user-service | order-service | Create new orders |
| `VIEW_ORDERS` | user-service | order-service | View order details & history |
| `PROCESS_PAYMENT` | user-service | payment-service, inventory-service | Process payments, reserve/release inventory |
| `VIEW_PAYMENTS` | user-service | payment-service | View payment details & history |
| `MODIFY_INVENTORY` | user-service | inventory-service | Create/update products and stock |
| `VIEW_INVENTORY` | user-service | inventory-service | View products, stock, transactions |
| `VIEW_USERS` | user-service | notification-service | Manage notifications, view preferences |

### Authority Combinations

| User Type | Authorities | Use Cases |
|-----------|-------------|-----------|
| Regular Customer | `CREATE_ORDER`, `VIEW_ORDERS`, `PROCESS_PAYMENT`, `VIEW_PAYMENTS` | View/create orders, make payments |
| Admin | All authorities | Full platform access |
| Inventory Manager | `MODIFY_INVENTORY`, `VIEW_INVENTORY` | Manage products and stock |
| Support Agent | `VIEW_ORDERS`, `VIEW_PAYMENTS`, `VIEW_INVENTORY` | View order/payment/inventory data |

---

## Request/Response Examples

### Create Order (with authorization)

**Request:**
```
POST /api/v1/orders HTTP/1.1
Host: order-service:9003
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "customerId": 123,
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

**Response (Success):**
```
HTTP/1.1 201 Created
Content-Type: application/json

{
  "status": 201,
  "data": {
    "orderId": "ORD-12345",
    "customerId": 123,
    "total": 99.99,
    "status": "PENDING"
  }
}
```

**Response (Unauthorized):**
```
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token"
}
```

**Response (Forbidden):**
```
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "error": "Forbidden",
  "message": "User lacks required authority: CREATE_ORDER"
}
```

---

## Token Format

Expected JWT token structure for successful authorization:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
.
{
  "sub": "user-123",
  "userId": 123,
  "email": "user@example.com",
  "authorities": [
    "CREATE_ORDER",
    "VIEW_ORDERS",
    "PROCESS_PAYMENT",
    "VIEW_PAYMENTS"
  ],
  "iss": "ecommerce-platform",
  "iat": 1694251200,
  "exp": 1694251900
}
```

### Token Claims

| Claim | Type | Required | Description |
|-------|------|----------|-------------|
| `sub` | String | Yes | Subject (username or user ID) |
| `userId` | Number | Yes | Numeric user ID |
| `email` | String | No | User email address |
| `authorities` | Array | Yes | List of granted authorities |
| `iss` | String | Yes | Issuer (must be "ecommerce-platform") |
| `iat` | Number | Yes | Issued at (Unix timestamp) |
| `exp` | Number | Yes | Expiration time (Unix timestamp) |

---

## Headers Required

### Authorization Header

All requests to protected endpoints must include:

```
Authorization: Bearer <token>
```

Where `<token>` is a valid JWT issued by user-service.

### Example:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyIsInVzZXJJZCI6MTIzLCJhdXRob3JpdGllcyI6WyJDUkVBVEVfT1JERVIiXSwiaXNzIjoiZWNvbW1lcmNlLXBsYXRmb3JtIiwiZXhwIjoxNjk0MjUxOTAwfQ.signature_here
```

---

## HTTP Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| `200` | OK | Successful GET request |
| `201` | Created | Successful POST/creation |
| `204` | No Content | Successful DELETE/operation with no response body |
| `400` | Bad Request | Invalid input/validation error |
| `401` | Unauthorized | Missing or invalid JWT |
| `403` | Forbidden | Valid JWT but user lacks authority |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Resource constraint violated (e.g., SKU exists) |
| `500` | Server Error | Unexpected server error |

---

## Testing Authorization

### Using cURL

**Without Authorization:**
```bash
curl -X GET http://localhost:9003/api/v1/orders
# Returns: 401 Unauthorized
```

**With Authorization:**
```bash
curl -X GET http://localhost:9003/api/v1/orders \
  -H "Authorization: Bearer $JWT_TOKEN"
# Returns: 200 OK with orders list
```

### Using Postman

1. Set Authorization type: `Bearer Token`
2. Paste JWT token in the token field
3. Send request

---

## Troubleshooting Authorization

### Issue: 401 Unauthorized

**Causes:**
- Missing Authorization header
- Invalid JWT format
- Expired token
- JWT secret mismatch

**Solution:**
- Add Authorization header with Bearer token
- Check token expiration
- Verify JWT secret in user-service matches downstream services

### Issue: 403 Forbidden

**Causes:**
- User doesn't have required authority
- Authority name mismatch in token

**Solution:**
- Check user's authorities in user-service
- Verify @PreAuthorize expression and authority names
- Ensure user-service issued authority with correct name

### Issue: Endpoint accessible without JWT

**Causes:**
- `application.security.enabled: false` in config
- Endpoint not annotated with @PreAuthorize
- Endpoint whitelisted as public

**Solution:**
- Enable security: `application.security.enabled: true`
- Add @PreAuthorize annotation to endpoint
- Review SecurityConfig whitelisting

