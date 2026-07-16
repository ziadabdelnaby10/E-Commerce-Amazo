# API Gateway: Central Request Router and Authentication

> Learn how API Gateway centralizes routing, authentication, rate limiting, and request/response transformation across your microservices.

## 📋 Overview

### What is an API Gateway?

**Without API Gateway**: Direct service access
```
┌─────────────┐
│ Mobile App  │
└─────┬───────┘
      │ (must know each service URL)
      ├─→ POST http://user-service:8001/auth
      ├─→ POST http://order-service:8002/orders
      └─→ GET  http://inventory-service:8003/products
```

**With API Gateway**: Single entry point
```
┌─────────────┐
│ Mobile App  │
└─────┬───────┘
      │ (single entry point)
      └─→ POST http://api.example.com/auth
          ↓
          ┌─────────────────────────┐
          │   API Gateway           │
          │ (Port 8889)             │
          │ - Route requests        │
          │ - Validate JWT          │
          │ - Rate limit            │
          │ - Log requests          │
          └─────┬──────────┬────────┘
                │          │
         ┌──────▼────┐  ┌──▼──────┐
         │ User Svc  │  │Order Svc │
         └───────────┘  └──────────┘
```

### API Gateway Responsibilities

| Responsibility | Example |
|---|---|
| **Routing** | `/auth/*` → User Service, `/orders/*` → Order Service |
| **Authentication** | Validate JWT on all requests |
| **Rate Limiting** | Max 100 requests/minute per user |
| **Logging** | Track all requests for audit |
| **Transformation** | Add user ID from JWT to request headers |
| **CORS** | Handle cross-origin requests |
| **Compression** | Gzip responses |
| **Caching** | Cache GET requests for 5 minutes |

## 🏗️ Role in Your Architecture

```
Client (Web/Mobile)
  ↓
API Gateway (Port 8889) ← Validates JWT here (single place)
  ├─ route_matcher /auth/** → User Service:8001
  ├─ route_matcher /orders/** → Order Service:8002
  ├─ route_matcher /products/** → Inventory Service:8003
  ├─ route_matcher /payments/** → Payment Service:8004
  └─ route_matcher /notifications/** → Notification Service:8005
```

**Benefits:**
- ✅ Clients don't know internal service URLs
- ✅ Centralized security (JWT validation in one place)
- ✅ Easy to add rate limiting across all services
- ✅ Service URLs can change without affecting clients
- ✅ Single point to log/monitor requests

## Configuration

### Setup: Spring Cloud Gateway

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
</dependency>
```

```yaml
# application.yml
server:
  port: 8889

spring:
  cloud:
    gateway:
      routes:
        # Route 1: User Service
        - id: user-service
          uri: http://localhost:8001
          predicates:
            - Path=/users/**,/auth/**
          filters:
            - StripPrefix=0
            
        # Route 2: Order Service (requires authentication)
        - id: order-service
          uri: http://localhost:8002
          predicates:
            - Path=/orders/**
          filters:
            - name: JwtAuthenticationFilter
            - StripPrefix=0
            
        # Route 3: Inventory Service
        - id: inventory-service
          uri: http://localhost:8003
          predicates:
            - Path=/products/**,/inventory/**
          filters:
            - name: JwtAuthenticationFilter
            
        # Route 4: Payment Service
        - id: payment-service
          uri: http://localhost:8004
          predicates:
            - Path=/payments/**
          filters:
            - name: JwtAuthenticationFilter
            
        # Route 5: Notification Service
        - id: notification-service
          uri: http://localhost:8005
          predicates:
            - Path=/notifications/**
          filters:
            - name: JwtAuthenticationFilter
```

### JWT Authentication Filter

```java
@Component
@Slf4j
public class JwtTokenFilter implements GlobalFilter {
    
    @Autowired
    private JwtProvider jwtProvider;
    
    // Routes that don't need authentication
    private static final List<String> PUBLIC_ROUTES = Arrays.asList(
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/users/register",
        "/users/login"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Skip authentication for public routes
        if (PUBLIC_ROUTES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        
        // Extract JWT from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        String token = authHeader.substring(7);
        
        // Validate token
        if (!jwtProvider.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
        
        // Extract user ID from token
        String userId = jwtProvider.extractUserId(token);
        Claims claims = jwtProvider.extractClaims(token);
        
        // Add user info to request headers (passed to downstream services)
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", userId)
            .header("X-User-Role", claims.get("role", String.class))
            .build();
        
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(modifiedRequest)
            .build();
        
        log.info("JWT validated for user: {} accessing: {}", userId, path);
        return chain.filter(modifiedExchange);
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        
        DataBuffer errorBuffer = response.bufferFactory().wrap(
            String.format("{\"error\":\"%s\"}", message).getBytes()
        );
        
        return response.writeWith(Mono.just(errorBuffer));
    }
}
```

### Rate Limiting

```java
@Configuration
public class RateLimitingConfig {
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders()
                .getFirst("X-User-Id")  // Set by JWT filter
        );
    }
}
```

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8002
          predicates:
            - Path=/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenish-rate: 100  # 100 requests per...
                  requested-tokens: 1
                key-resolver: "#{@userKeyResolver}"  # Per user
```

### Request/Response Logging

```java
@Configuration
public class LoggingConfig {
    
    @Bean
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            long startTime = System.currentTimeMillis();
            
            log.info("REQUEST: {} {} from {}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress()
            );
            
            return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("RESPONSE: {} ms, status: {}",
                        duration,
                        exchange.getResponse().getStatusCode()
                    );
                })
            );
        };
    }
}
```

### CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000", "https://example.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

## Monitoring

### Health Check

```java
@RestController
public class ActuatorController {
    
    @GetMapping("/gateway/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("routes", Arrays.asList(
            "/users/** (User Service)",
            "/orders/** (Order Service)",
            "/products/** (Inventory Service)"
        ));
        return ResponseEntity.ok(health);
    }
}
```

### Prometheus Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    distribution:
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

## Troubleshooting

### Services Behind Gateway Not Responding

❌ **Problem**: Gateway returns 503 Service Unavailable
```
Cause: Service URL incorrect or service down
```

✅ **Solution**: Verify services running
```bash
curl http://localhost:8001/actuator/health  # User Service
curl http://localhost:8002/actuator/health  # Order Service
```

### JWT Validation Failing

❌ **Problem**: All requests return 401 Unauthorized
```
Cause: Secret key mismatch between services
```

✅ **Solution**: Ensure same JWT secret
```yaml
# All services must have same secret from environment
jwt:
  secret: ${JWT_SECRET}
```

### Rate Limiting Not Working

❌ **Problem**: Can make unlimited requests
```
Cause: Redis not connected
```

✅ **Solution**: Check Redis connection
```bash
docker logs ecommerce-redis
redis-cli ping  # Should return PONG
```

## 🔗 Resources

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Kong API Gateway](https://konghq.com/) (alternative)
- Your Project: `gateway-service` folder contains gateway implementation

---

**Next**: Read [Discovery Service](07-Discovery-Service.md) for dynamic service registration, or [Resilience4j](13-Resilience4j.md) for fault tolerance.

