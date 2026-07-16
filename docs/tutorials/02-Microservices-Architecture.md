# Microservices Architecture

> Master the principles of distributed systems design: service independence, inter-service communication, data consistency, and resilience patterns that define your E-Commerce platform.

## 📋 Concepts

### Monolith vs Microservices

**Monolith**
```
┌─────────────────────────────┐
│  Single Codebase            │
├─────────────────────────────┤
│ │ User Service              │
│ │ Order Service             │
│ │ Inventory Service         │
│ │ Payment Service           │
├─────────────────────────────┤
│ Shared PostgreSQL Database  │
└─────────────────────────────┘
```
- Simple to develop initially
- Tight coupling: change one feature → risk breaking others
- Scaling: must scale entire monolith even if only Order Service needs it
- Tech lock-in: all services use same language/framework

**Microservices**
```
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│  User     │  │  Order    │  │Inventory  │  │ Payment   │
│  Service  │  │  Service  │  │  Service  │  │  Service  │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │              │
   user_db       order_db       inventory_db   payment_db
```
- Independent deployment: update Order without restarting User Service
- Loose coupling: services communicate via APIs/events
- Technology flexibility: User Service can be Java, Order Service can be Go
- Easy scaling: scale just Inventory Service during flash sales

### The Database-Per-Service Pattern

**Why not share one database?**

❌ **Shared Database (Tight Coupling)**
```
Services can query each other's tables directly
Problem: Direct table access = service interdependence
Example: Order Service queries user_profiles table directly
         → Must know User Service's database schema
         → User Service can't change schema without breaking Order
```

✅ **Database-Per-Service (Loose Coupling)**
```
Each service owns its database
Services access each other only via REST API/Kafka
Example: Order Service needs user info
         → Calls REST: GET /users/123/details
         → Only needs to know the API contract (DTO)
         → User Service can change internal database freely
```

**Your Project:**
- User Service → postgres/user_db
- Order Service → postgres/order_db
- Inventory Service → postgres/inventory_db
- Payment Service → postgres/payment_db
- Notification Service → postgres/notification_db

### Service Communication Patterns

#### 1. Synchronous (Request-Response)

```
Client              API Gateway         Order Service      Inventory Service
  │                    │                    │                    │
  │ POST /orders       │                    │                    │
  ├──────────────────>│                    │                    │
  │                    │ route to Order    │                    │
  │                    ├──────────────────>│                    │
  │                    │                    │ Check stock       │
  │                    │                    ├───────────────────>│
  │                    │                    │ 200 OK (reserved) │
  │                    │                    │<───────────────────┤
  │                    │ 200 Order Created │                    │
  │                    │<──────────────────┤                    │
  │ { orderId: 123}    │                    │                    │
  │<──────────────────┤                    │                    │
```

**When to use:**
- Immediate feedback needed (user clicks "Place Order" → needs confirmation)
- Read operations (get product details)
- Validation (is payment valid?)

**Code Example:**
```java
@Service
public class OrderService {
    @Autowired
    private RestTemplate restTemplate;  // or WebClient
    
    public Order createOrder(OrderRequest request) {
        // Synchronous call: blocks until response
        InventoryResponse stock = restTemplate.getForObject(
            "http://inventory-service:8003/products/{id}/stock",
            InventoryResponse.class,
            request.getProductId()
        );
        
        if (stock.getAvailable() < request.getQuantity()) {
            throw new OutOfStockException();
        }
        
        return orderRepository.save(new Order(request));
    }
}
```

#### 2. Asynchronous (Event-Driven)

```
Order Service          Kafka          Inventory Service    Notification Service
    │                   │                    │                    │
    │ OrderCreated event│                    │                    │
    ├──────────────────>│                    │                    │
    │ (returns to client)                    │                    │
    │                   │ partition 0        │                    │
    │                   ├──────────────────>│                    │
    │                   │ partition 1        │                    │
    │                   ├──────────────────────────────────────>│
    │                   │                    │ reserve stock      │
    │                   │                    │ (no response)      │
    │                   │                    │ send email         │
```

**When to use:**
- Operations that don't need immediate feedback
- Loose coupling required
- Multiple services need to react to same event

**Code Example:**
```java
@Service
public class OrderEventPublisher {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(order.getId(), "OrderCreated", order);
        // Non-blocking: returns immediately
        kafkaTemplate.send("order-events", order.getId(), event);
    }
}

@Service
public class InventoryEventListener {
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void onOrderCreated(OrderEvent event) {
        // Processes asynchronously, multiple events processed in parallel
        inventoryService.reserveStock(event.getOrder());
    }
}
```

## 🏗️ Implementation in Your Project

### Current Architecture

```
┌────────────────────────────────────────────────┐
│         API Gateway (8889)                      │
│  (Route, Authenticate, Rate Limit)             │
└────────┬──────────────┬──────────────┬─────────┘
         │              │              │
    ┌────▼───┐   ┌─────▼────┐  ┌─────▼────┐
    │ User   │   │ Order    │  │ Discovery│
    │Svc 8001│   │ Svc 8002 │  │ Svc 8761 │
    └────┬───┘   └────┬─────┘  └──────────┘
         │            │
    ┌────▼────────────▼─────────────────────┐
    │    Kafka (Message Bus)                 │
    │  order-events | payment-events         │
    └────────────────────────────────────────┘
         │            │            │
    ┌────▼───┐  ┌────▼─────┐  ┌──▼─────┐
    │Inventory│  │ Payment  │  │Notif.  │
    │Svc 8003 │  │ Svc 8004 │  │Svc 8005│
    └────┬────┘  └────┬─────┘  └────────┘
         └────────────┴─────┬──────────┘
              │              │
        ┌─────▼─────┐   ┌────▼────┐
        │ PostgreSQL│   │  Redis   │
        │ (5 DBs)   │   │  Cache   │
        └───────────┘   └──────────┘
```

### Service Boundaries (Domain-Driven Design)

Each service owns a **bounded context** — a business domain it's responsible for:

| Service | Responsibility | Database | Kafka Topics |
|---------|-----------------|----------|--------------|
| **User Service** | Authentication, user profiles, roles | user_db | user-events |
| **Order Service** | Order CRUD, saga coordination | order_db | order-events |
| **Inventory Service** | Product catalog, stock tracking | inventory_db | inventory-events |
| **Payment Service** | Payment processing, encryption | payment_db | payment-events |
| **Notification Service** | Email/SMS sending, event archive | notification_db | All topics |

**Key Rule**: Services never bypass API/Kafka. Inventory Service needs user info? Call User Service REST endpoint, don't query user_db directly.

### Building Blocks: Each Service Has 5 Layers

```
Order Service (Port 8002)
│
├─ Controller (REST API)
│  ├── POST /orders (create)
│  ├── GET /orders/{id} (read)
│  └── DELETE /orders/{id} (delete)
│
├─ Service (Business Logic)
│  ├── validateOrder()
│  ├── createOrder()
│  └── publishEvent()
│
├─ Repository (Data Access)
│  ├── save(), findById(), delete()
│  └── custom queries
│
├─ Entity (Data Model)
│  ├── Order (JPA @Entity)
│  └── OrderItem
│
└─ Event Handler (Kafka)
   ├── onPaymentCompleted()
   └── onInventoryReserved()
```

### Example: Order Creation Flow (Synchronous + Asynchronous)

```
1. POST /orders (User clicks Place Order)
   └─> OrderController.createOrder()
       
2. Validate order
   └─> OrderService.validateOrder()
       └─> REST call to Inventory: GET /products/{id}/check-stock
           └─> Inventory Service checks stock, returns OK
           
3. Save order to database
   └─> OrderRepository.save()
       └─> INSERT INTO orders VALUES (...)
       
4. Return immediately to client
   └─> JSON: { "orderId": "123", "status": "PENDING" }
   
5. (Background) Publish event
   └─> KafkaTemplate.send("order-events", "OrderCreated")
   
6. (Background) Inventory Service consumes
   └─> InventoryService.onOrderCreated()
       └─> Reserve stock
       └─> UPDATE products SET quantity = quantity - 1
       
7. (Background) Notification Service consumes
   └─> NotificationService.sendConfirmationEmail()
```

## ⚙️ When to Use Each Pattern

### Use Synchronous (REST) When:
- ✅ Immediate response required
- ✅ Validation needed before proceeding
- ✅ Strong consistency required
- ✅ Small operations (get user email)

**Example**: Order Service → Inventory Service (check stock before creating order)

### Use Asynchronous (Kafka) When:
- ✅ Fire-and-forget operations
- ✅ Multiple services need to react
- ✅ Can tolerate eventual consistency
- ✅ Long-running operations
- ✅ Audit trail needed

**Example**: Order Service publishes OrderCreated → Inventory, Payment, Notification all react

## 🔴 Common Pitfalls

### 1. Distributed Transaction Problem

❌ **Problem**: Need to guarantee all-or-nothing across services
```
Order Service saves order
Inventory Service fails to reserve stock
→ Order exists but no inventory reserved → Inconsistent state
```

✅ **Solution**: Saga Pattern (orchestrated transactions)
```
Order Service → (start saga)
  → Call Inventory: reserve stock
     If OK → Call Payment: process payment
        If OK → Publish OrderCreated (commit)
        If Fail → Publish OrderCancelled (rollback)
```

### 2. Service Dependency Hell

❌ **Problem**: Services tightly coupled
```
User Service → Order Service → Inventory Service → Payment Service → Notification Service
Kill Notification Service → entire chain breaks
```

✅ **Solution**: Use circuit breakers (Resilience4j)
```java
@CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
public Stock checkStock(String productId) {
    return restTemplate.getForObject(
        "http://inventory:8003/stock/{id}", 
        Stock.class, 
        productId
    );
}

public Stock inventoryFallback(String productId) {
    // Return cached stock or default
    return new Stock(productId, 0);
}
```

### 3. Data Consistency Across Services

**Problem**: User updates email in User Service, Order Service still has old email

**Solution**: Use events for eventual consistency
```
1. User Service: email updated → Publish UserEmailUpdated event
2. Order Service: receives event → UPDATE cached user email
3. Notification Service: receives event → UPDATE cached user email
```

### 4. Monitoring & Debugging

**Problem**: Order created successfully but customer didn't get email (Notification Service broke silently)

**Solution**: Implement observability
- Logs: Each service logs all operations
- Tracing: Follow request across services (Spring Cloud Sleuth)
- Alerts: Monitor dead-letter queues for failed messages

## 🔗 Resources

- [Building Microservices by Sam Newman](https://samnewman.io/books/building_microservices/)
- [Your Project ADR-001](../ADR-001-Microservices-Architecture.md) — Architecture decisions
- [Your Project ADR-002](../ADR-002-Kafka-Events-Design.md) — Event architecture
- Spring Boot [Microservices Patterns](https://microservices.io/patterns/index.html)

---

**Next**: Read [Kafka](01-Kafka.md) for event-driven implementation details, or [API Gateway](05-API-Gateway.md) to understand request routing.

