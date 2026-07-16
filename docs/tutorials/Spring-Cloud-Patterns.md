# Spring Cloud Patterns: Distributed System Patterns

> Master enterprise patterns for building distributed systems: service discovery, configuration management, load balancing, and circuit breakers coordinated through Spring Cloud.

## 📋 Patterns Overview

### Service Discovery Pattern

**Problem**: Hard-coded service URLs break in microservices
```
OrderService needs to call InventoryService
Hard-coded: http://localhost:8003 (breaks if moved to different server)
```

**Solution**: Dynamic discovery via Eureka
```
1. InventoryService registers with Eureka on startup
2. OrderService queries Eureka: "where is InventoryService?"
3. Eureka returns: "http://10.0.1.5:8003"
4. OrderService calls dynamically
```

**Your Project**: See [Discovery Service](07-Discovery-Service.md)

### Configuration Server Pattern

**Problem**: Configuration scattered across services
```
Database URL in user-service/application.yml
Database URL in order-service/application.yml
Database URL in inventory-service/application.yml
 → Need to update 3 files when DB host changes
```

**Solution**: Centralized Configuration Server
```
Central Config Server stores all configs
All services fetch config on startup from Config Server
Change one file, all services pick it up (or on next restart)
```

**Implementation**:
```yaml
# config-server/application.yml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/yourrepo/config-repo
          default-label: main

# format: /{appname}/{profile}/[{label}]
# Example: GET /user-service/prod/main
```

```yaml
# Services: bootstrap.yml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      name: user-service  # Config file: user-service.yml
      profile: ${PROFILE:dev}
```

### Load Balancing Pattern

**Without Load Balancer**: Direct calls unbalanced
```
Order Service
  ├─ Call Inventory-1
  ├─ Call Inventory-1  ← Always same instance
  ├─ Call Inventory-1
  └─ Inventory-1 overloaded, Inventory-2 idle
```

**With Load Balancer**: Distributed calls
```
Order Service with @LoadBalanced RestTemplate
  ├─ Call Inventory-1  (first request)
  ├─ Call Inventory-2  (second request, round-robin)
  ├─ Call Inventory-1  (third request)
  └─ Balanced load across instances
```

**Code**:
```java
@Configuration
public class LoadBalancingConfig {
    
    @Bean
    @LoadBalanced  // Enables client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// Usage: service name, not URL
restTemplate.getForObject("http://inventory-service/products/123", Product.class);
```

### Distributed Tracing Pattern

**Problem**: Request fails → which service caused it?
```
Client → API Gateway → Order Service → Inventory Service → Payment Service
                            ❌ Failed here?
Need to trace request through all services
```

**Solution**: Spring Cloud Sleuth + Zipkin
```java
@Configuration
public class TracingConfig {
    @Bean
    public Sampler defaultSampler() {
        return Sampler.ALWAYS_SAMPLE;  // Sample all requests
    }
}

// All logs automatically include:
// - traceId: unique ID for request across services
// - spanId: this service's part of request
// - spanExportId: parent service (for tracing)
```

**Logs show**:
```
2026-07-16 10:00:00 [user-service,5c4ecc49f0f8a62e,5c4ecc49f0f8a62e,false] 
                     ^^^^^^^ service ^^^^^^^^^^^ trace ID ^^^^^^^^^^^^^ span ID
```

### Circuit Breaker Pattern

**See [Resilience4j](13-Resilience4j.md) tutorial** - Isolates failures, prevents cascading

### Saga Pattern

**Problem**: Distributed transaction (Order → Inventory → Payment)
```
Order created ✓
Inventory reserved ✓
Payment failed ❌
→ Order exists but payment failed → inconsistent state
```

**Solution**: Saga (choreography or orchestration)

**Choreography (Event-driven)**:
```
Order Service creates order, publishes OrderCreated
  ↓ (via Kafka)
Inventory Service consumes, reserves stock, publishes InventoryReserved
  ↓ (via Kafka)
Payment Service consumes, processes payment, publishes PaymentCompleted
  ↓ (via Kafka)
Order Service consumes PaymentCompleted, confirms order
```

**Orchestration (Centralized)**:
```
Order Saga Orchestrator:
  1. Tell Inventory Service: reserve stock
  2. If OK → Tell Payment Service: process payment
  3. If OK → Confirm order
  4. If fail → Compensating transaction: release inventory
```

### Bulkhead Pattern

**Problem**: One slow service starves others
```
Notification Service slow (sending emails)
  → All notification threads blocked
  → Order Service thread pool exhausted waiting for notification
  → Order Service becomes unresponsive
```

**Solution**: Isolate threads per service
```java
@Service
public class OrderService {
    
    @Bulkhead(name = "orderBulkhead", type = Bulkhead.Type.THREADPOOL)
    public Order createOrder(OrderRequest request) {
        // Uses separate thread pool, doesn't affect other services
        return orderRepository.save(mapToEntity(request));
    }
}

@Service
public class NotificationService {
    
    @Bulkhead(name = "notificationBulkhead", type = Bulkhead.Type.THREADPOOL)
    public void sendEmail(EmailRequest email) {
        // Separate thread pool, if slow doesn't affect Order Service
        mailClient.send(email);
    }
}
```

## Implementation: Putting It All Together

### Service Registration on Startup

```
App starts → Eureka Client active
  ├─ Register: "order-service" at "localhost:8002"
  ├─ Fetch Config: POST http://config-server:8888/order-service/dev
  └─ Ready to serve → http://localhost:8089 (API Gateway)
```

### Request Flow with All Patterns

```
1. Client → API Gateway (8889)
   └─ Gateway validates JWT
   
2. API Gateway → Discovery Server (Eureka 8761)
   └─ "Route /orders/** to order-service"
   └─ Discovered: order-service at localhost:8002
   
3. API Gateway → Order Service (8002)
   └─ Headers include: X-Trace-Id (Sleuth)
   
4. Order Service → Inventory Service
   └─ Via @LoadBalanced RestTemplate
   └─ Via Circuit Breaker (Resilience4j)
   └─ Logs include trace ID
   └─ If fails: fallback
   
5. Order Service → Kafka
   └─ Publishes OrderCreated event
   
6. Inventory/Payment/Notification Services consume event
   └─ Each has Bulkhead isolation
   └─ Each updates via Saga pattern
```

## Configuration Best Practices

### application.yml Hierarchy

```
# Common across all environments
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://config-server:8888

---
# Development profile
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/order_db_dev
  kafka:
    bootstrap-servers: localhost:9092

---
# Production profile
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://postgres.prod.example.com:5432/order_db
    hikari:
      maximum-pool-size: 20
  kafka:
    bootstrap-servers: kafka.prod.example.com:9092
```

### Running with Profiles

```bash
# Development (local)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Production
java -jar order-service.jar --spring.profiles.active=prod
```

## Observability (Monitoring All Patterns)

```yaml
# Enable all observation
management:
  observations:
    enable:
      http.server.requests: true
      spring.security.http.requests: true
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests (costs performance)
```

## 🔗 Resources

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- [Zipkin Distributed Tracing](https://zipkin.io/)
- Microservices Patterns Book: patterns for distributed systems
- Your Project: Uses multiple Spring Cloud patterns

---

**Next**: Read [Docker & Deployment](Docker-Deployment.md) for containerizing and deploying services, or revisit any specific pattern tutorial.

