# Resilience4j: Fault Tolerance and Circuit Breakers

> Master fault tolerance patterns: circuit breakers, retries, timeouts, and bulkheads to build resilient microservices that gracefully handle failures.

## 📋 Core Concepts

### The Problem: Cascading Failures

```
Customer places order:
- Order Service calls Inventory Service (OK)
- Inventory Service calls Payment Service (OK)
- Payment Service is down ❌
- Request hangs for 30 seconds...
- Thread exhausted
- Order Service becomes unresponsive
- API Gateway timeouts
- Entire system affected
```

**Solution**: Fail fast and isolate failures
```
Customer places order:
- Order Service calls Inventory Service via Circuit Breaker
- Inventory Service calls Payment Service via Circuit Breaker
- Payment Service is down ❌
- Circuit Breaker opens immediately (fail fast)
- Return cached response or error
- Resources freed for next request
- System stays responsive
```

### Circuit Breaker States

```
┌─────────────────────────────────────────────────┐
│                CLOSED (Normal)                  │
│ Successful calls pass through                   │
└────────┬────────────────────────────────────────┘
         │ 5 failures out of 10 calls
         ↓
┌─────────────────────────────────────────────────┐
│                OPEN (Stop calling)              │
│ Calls immediately return error/fallback         │
│ No time wasted calling failed service           │
└────────┬────────────────────────────────────────┘
         │ Wait 30 seconds (timeout)
         ↓
┌─────────────────────────────────────────────────┐
│           HALF-OPEN (Test recovery)             │
│ Allow 1-2 test calls through                    │
│ If successful → CLOSED, if fail → OPEN         │
└─────────────────────────────────────────────────┘
```

## How It Works

```
Call sequence:
1. Order Service → Circuit Breaker → Inventory Service
                       ↓
                   Call count++
                       ↓
                Success? → CLOSED state
                       ↑
Failure count > threshold?
                   ↓ YES
                Call count reset
                Open circuit
                Return fallback value
```

## 🏗️ Implementation in Your Project

### Setup Resilience4j

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-timelimiter</artifactId>
</dependency>
```

### 1. Circuit Breaker: Call External Service

```java
@Service
@Slf4j
public class InventoryServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Call Inventory Service with circuit breaker protection
     * Fails fast if service unavailable
     */
    @CircuitBreaker(
        name = "inventoryService",
        fallbackMethod = "getProductFallback"
    )
    public Product getProduct(String productId) {
        log.info("Calling Inventory Service for product: {}", productId);
        
        return restTemplate.getForObject(
            "http://inventory-service/products/{id}",
            Product.class,
            productId
        );
    }
    
    /**
     * Fallback: Called when circuit breaker is open
     * Return cached/default value instead of failing
     */
    private Product getProductFallback(String productId, Exception e) {
        log.warn("Circuit breaker fallback for product: {}", productId, e);
        
        // Option 1: Return cached product
        return productCache.get(productId);
        
        // Option 2: Return default product
        // return new Product(productId, "Unavailable", BigDecimal.ZERO);
        
        // Option 3: Return empty
        // return null;
    }
}
```

### 2. Retry: Resilient to Transient Failures

```java
@Service
public class PaymentServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Retry on failure (network hiccup, temporary unavailability)
     * Waits between retries with exponential backoff
     */
    @Retry(
        name = "paymentService",
        fallbackMethod = "processPaymentFallback"
    )
    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(
            "http://payment-service/payments",
            request,
            PaymentResponse.class
        );
    }
    
    /**
     * Called after all retries exhausted
     */
    private PaymentResponse processPaymentFallback(
        PaymentRequest request, 
        Exception e) {
        log.error("Payment failed after retries", e);
        throw new PaymentProcessingException("Payment service unavailable");
    }
}
```

### 3. Timeout: Prevent Hanging Requests

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Timeout: If call takes > 2 seconds, abort
     * Returns TimeoutException
     */
    @TimeLimiter(
        name = "orderService",
        fallbackMethod = "createOrderFallback"
    )
    public CompletableFuture<Order> createOrder(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Long-running operation
            return processOrder(request);
        });
    }
    
    private CompletableFuture<Order> createOrderFallback(
        OrderRequest request, 
        TimeoutException e) {
        log.warn("Order creation timed out");
        return CompletableFuture.failedFuture(e);
    }
}
```

### 4. Bulkhead: Limit Concurrent Calls

```java
@Service
public class NotificationService {
    
    /**
     * Bulkhead: Max 10 concurrent requests to email service
     * 11th request queued or rejected
     * Prevents thread pool exhaustion
     */
    @Bulkhead(
        name = "emailService",
        type = Bulkhead.Type.THREADPOOL,
        fallbackMethod = "sendEmailFallback"
    )
    public void sendEmail(EmailRequest email) {
        // CPU-bound work in thread pool
        mailClient.send(email);
    }
    
    private void sendEmailFallback(EmailRequest email, Exception e) {
        log.warn("Email sending failed, queuing for retry: {}", email.getTo());
        emailQueue.add(email);  // Queue for later
    }
}
```

### Configuration: application.yml

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10  # Evaluate last 10 calls
        failureRateThreshold: 50  # If 50%+ fail, open circuit
        waitDurationInOpenState: 30000  # Wait 30s before trying again
        minimumNumberOfCalls: 5  # Need at least 5 calls to evaluate
        automaticTransitionFromOpenToHalfOpenEnabled: true
        
      paymentService:
        slidingWindowSize: 20
        failureRateThreshold: 60
        waitDurationInOpenState: 60000
        
  retry:
    instances:
      paymentService:
        max-attempts: 3
        wait-duration: 1000  # 1 second between retries
        retry-exceptions:
          - java.net.ConnectException
          - java.io.IOException
        ignore-exceptions:
          - com.ecommerce.BusinessException  # Don't retry
          
  timelimiter:
    instances:
      orderService:
        timeoutDuration: 2s
        cancelRunningFuture: true
        
  bulkhead:
    instances:
      emailService:
        maxConcurrentCalls: 10
        maxWaitDuration: 10ms
        metrics:
          enabled: true
```

### Combining Patterns: Comprehensive Resilience

```java
@Service
@Slf4j
public class OrderServiceWithFullResilience {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * All patterns combined:
     * 1. CircuitBreaker: Fail fast if inventory unavailable
     * 2. Retry: Handle transient failures
     * 3. TimeLimiter: Don't wait forever
     * 4. Bulkhead: Limit concurrent calls
     */
    @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory")
    @TimeLimiter(name = "inventory")
    @Bulkhead(name = "inventory")
    public Product checkInventory(String productId) {
        log.info("Checking inventory for: {}", productId);
        
        return restTemplate.getForObject(
            "http://inventory-service/products/{id}/stock",
            Product.class,
            productId
        );
    }
    
    // Fallback handles all failures
    private Product inventoryFallback(String productId, Exception e) {
        log.error("Failed to check inventory for: {}, using cache", productId, e);
        
        // Return from cache instead of failing
        return productCache.getOrDefault(productId, 
            new Product(productId, "stock-unknown", BigDecimal.ZERO));
    }
}
```

## Monitoring & Metrics

### Enable Metrics Endpoint

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Check Circuit Breaker Status

```bash
# Get metrics
curl http://localhost:8002/actuator/metrics

# Get specific circuit breaker status
curl http://localhost:8002/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:inventoryService

# Response:
{
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 1  # 1=CLOSED, 2=OPEN, 3=HALF_OPEN
    }
  ]
}
```

### Health Check Shows Circuit Breaker Status

```json
GET /actuator/health
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "inventoryService": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": 0.0,
            "slowCallRate": 0.0
          }
        }
      }
    }
  }
}
```

## Common Patterns

### Pattern 1: Retry + Circuit Breaker

**Use when**: Transient failures possible
```
1. Try call
2. If fails → Retry (network hiccup)
3. If still fails → Circuit Breaker opens (persistent failure)
```

### Pattern 2: Timeout + Fallback

**Use when**: Long-running operations
```
1. Start async call
2. If > timeout → Abort
3. Return cached/default value
4. User doesn't notice
```

### Pattern 3: Bulkhead + Circuit Breaker

**Use when**: Resource-constrained
```
1. Limit concurrent calls (bulkhead)
2. If all threads busy → Queue request
3. If service down → Circuit Breaker blocks
```

## 🔗 Resources

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Spring Boot Resilience4j Integration](https://resilience4j.readme.io/docs/spring-boot-usage)
- Your Project: Used in `order-service` for calling external services

---

**Next**: Read [Spring Cloud Patterns](Spring-Cloud-Patterns.md) for distributed patterns, or [Testing Strategies](Testing-Strategies.md) for testing resilient code.

