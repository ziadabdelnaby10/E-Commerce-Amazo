# Discovery Service: Dynamic Service Registration

> Learn how Discovery Service enables automatic service registration and discovery, eliminating hardcoded service URLs in microservices.

## 📋 Overview

### The Problem: Hardcoded Service URLs

**Without Discovery Service** (Brittle)
```java
// Order Service code
@Value("${inventory.service.url:http://localhost:8003}")  // Hardcoded
private String inventoryServiceUrl;

// If Inventory Service moves to different server, code must change
```

**With Discovery Service** (Dynamic)
```java
// Order Service just asks Discovery Service "where is inventory-service?"
// No hardcoding needed
DiscoveryClient discoveryClient;
List<ServiceInstance> instances = discoveryClient.getInstances("inventory-service");
String url = instances.get(0).getUri().toString();
```

### Why It Matters for Microservices

In production with Kubernetes:
```
Inventory Service instances:
- Instance 1: pod-123.cluster.k8s (might restart)
- Instance 2: pod-456.cluster.k8s (new instance added)
- Instance 3: pod-789.cluster.k8s (might be removed)

Order Service needs to:
- Automatically discover all Inventory instances
- Load balance between them
- Remove failed instances from pool
- Add new instances when they start
```

## 🏗️ Role in Your Architecture

```
Startup Sequence:
1. User Service starts
   └─ Registers: ("user-service", "localhost:8001") with Discovery Server
   
2. Order Service starts
   └─ Registers: ("order-service", "localhost:8002") with Discovery Server
   
3. Inventory Service starts
   └─ Registers: ("inventory-service", "localhost:8003") with Discovery Server

4. Order Service wants to call Inventory
   └─ Queries Discovery Server: "who provides inventory-service?"
   └─ Gets: "localhost:8003"
   └─ Calls: "http://localhost:8003/products"

Discovery Server (Eureka on Port 8761):
┌─────────────────────────────────┐
│ Service Registry                │
├─────────────────────────────────┤
│ user-service       → 8001       │
│ order-service      → 8002       │
│ inventory-service  → 8003       │
│ payment-service    → 8004       │
│ notification-service → 8005     │
└─────────────────────────────────┘
```

## Configuration

### 1. Run Discovery Server (Eureka)

```bash
# docker-compose.yml - Already configured
discovery-server:
  image: openjdk:21-alpine
  environment:
    server.port: 8761
    eureka.server.enable-self-preservation: false
  ports:
    - "8761:8761"
```

Access at: http://localhost:8761

### 2. Register Microservices as Eureka Clients

**Dependency** (all services):
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Application.yml** (User Service example):
```yaml
spring:
  application:
    name: user-service  # Service name for registry
    
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: localhost
    port: 8001
    prefer-ip-address: false
    lease-renewal-interval-in-seconds: 30  # Heartbeat frequency
    lease-expiration-duration-in-seconds: 90  # Timeout to remove

server:
  port: 8001
```

**Main Class**:
```java
@SpringBootApplication
@EnableEurekaClient  // Registers this service
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 3. Call Other Services Using Discovery

**Method 1: Low-level DiscoveryClient**
```java
@Service
@Slf4j
public class InventoryServiceClient {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Product getProduct(String productId) {
        // Query Discovery Server for inventory-service instances
        List<ServiceInstance> instances = 
            discoveryClient.getInstances("inventory-service");
        
        if (instances.isEmpty()) {
            throw new ServiceNotAvailableException("inventory-service");
        }
        
        // Load balance: use first available instance
        ServiceInstance instance = instances.get(0);
        String url = instance.getUri() + "/products/" + productId;
        
        log.info("Calling Inventory Service at: {}", url);
        
        return restTemplate.getForObject(url, Product.class);
    }
}
```

**Method 2: High-level LoadBalanced RestTemplate** (Recommended)
```java
@Configuration
public class RestClientConfig {
    
    @Bean
    @LoadBalanced  // Enables client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class InventoryServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;  // Already load-balanced
    
    public Product getProduct(String productId) {
        // Spring automatically:
        // 1. Queries Eureka for "inventory-service" instances
        // 2. Load balances between them
        // 3. Retries on failure
        
        return restTemplate.getForObject(
            "http://inventory-service/products/{id}",  // Service name, not URL!
            Product.class,
            productId
        );
    }
}
```

## Monitoring

### Eureka Dashboard

Visit: http://localhost:8761

```
Dashboard shows:
├─ DS Replicas: [current instance]
├─ Registered Instances:
│  ├─ USER-SERVICE (1 instance)
│  │  └─ 127.0.0.1:user-service:8001
│  ├─ ORDER-SERVICE (1 instance)
│  │  └─ 127.0.0.1:order-service:8002
│  └─ INVENTORY-SERVICE (1 instance)
│     └─ 127.0.0.1:inventory-service:8003
└─ System Status:
   └─ Environment: [prod]
```

### Check Registration via REST

```bash
# Get all registered services
curl http://localhost:8761/eureka/apps

# Get specific service
curl http://localhost:8761/eureka/apps/inventory-service

# Response format:
{
  "applications": {
    "application": [{
      "name": "INVENTORY-SERVICE",
      "instance": [{
        "instanceId": "localhost:inventory-service:8003",
        "hostName": "localhost",
        "port": 8003,
        "status": "UP"
      }]
    }]
  }
}
```

### Health Checks

Services send heartbeat every 30 seconds:
```
Inventory Service → Eureka: "I'm alive"
Eureka: ACK

If 3 heartbeats missed (90 seconds):
Eureka removes Inventory Service from registry
Order Service stops routing to it
```

## Troubleshooting

### Service Not Appearing in Eureka Dashboard

❌ **Problem**: Service started but not showing in Eureka
```
Check logs for:
"Registering application USER-SERVICE with eureka"
```

✅ **Solution**: 
1. Verify `@EnableEurekaClient` annotation on main class
2. Check `eureka.client.service-url` points to Discovery Server
3. Ensure service name configured: `spring.application.name`

```bash
# Check if service registered
curl http://localhost:8761/eureka/apps/user-service
```

### "Cannot Find Service"

❌ **Problem**: Order Service can't find Inventory Service
```
java.net.UnknownHostException: inventory-service
```

✅ **Solution**:
1. Verify Inventory Service is running and registered
2. Check service name spelling (case-sensitive)
3. Use `@LoadBalanced` RestTemplate

```java
// ❌ Wrong (direct URL)
restTemplate.getForObject("http://localhost:8003/...", ..);

// ✅ Right (service name)
restTemplate.getForObject("http://inventory-service/...", ..);
```

### Eureka Server Not Starting

❌ **Problem**: Port 8761 in use

✅ **Solution**:
```bash
# Kill process on port 8761
lsof -i :8761  # Find process ID
kill -9 <PID>

# Or change port in application.yml
server:
  port: 8762 (new port)
```

## 🔗 Resources

- [Spring Cloud Netflix Eureka](https://spring.io/projects/spring-cloud-netflix)
- [Eureka Documentation](https://github.com/Netflix/eureka/wiki)
- [Service Discovery Patterns](https://microservices.io/patterns/service-registry.html)
- Your Project: Discovery Server on port 8761

---

**Next**: Read [Zookeeper](10-Zookeeper.md) for Kafka coordination, or [Resilience4j](13-Resilience4j.md) for handling service failures.

