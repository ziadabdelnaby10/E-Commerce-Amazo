# Kafka Event-Driven Architecture

> Learn how Kafka powers asynchronous communication in your microservices, enabling loose coupling and eventual consistency patterns.

## 📋 Concepts

### What is Kafka?

Apache Kafka is a distributed event streaming platform designed for:
- **High-throughput**: Handle millions of events per second
- **Low-latency**: Real-time event processing
- **Durability**: Events persisted to disk
- **Scalability**: Horizontal scaling via partitions and replicas

### Key Terminologies

| Term | Definition | Your Project |
|------|-----------|--------------|
| **Topic** | Category/channel for events | `order-events`, `payment-events` |
| **Partition** | Subset of topic for parallelism | 3 partitions per topic for scalability |
| **Producer** | Publishes events to topic | Order Service publishes OrderCreated |
| **Consumer** | Subscribes to topic events | Inventory Service listens for OrderCreated |
| **Consumer Group** | Multiple consumers sharing work | `inventory-service-orders` group |
| **Offset** | Position in partition stream | Kafka tracks where each consumer is |
| **Message Key** | Partitioning hint (optional) | Order ID ensures same order always goes to same partition |

### Event-Driven vs Request-Response

**Request-Response (Synchronous)**
```
Client → Order Service → Inventory Service → Payment Service → Response
```
- Fast feedback
- Strong consistency
- Coupling: if Inventory fails, Order fails
- Blocking calls

**Event-Driven (Asynchronous)**
```
Order Service → Kafka (OrderCreated) → [Multiple Consumers]
                                      ├→ Inventory Service
                                      ├→ Payment Service
                                      └→ Notification Service
```
- Loose coupling: services don't know about each other
- Eventual consistency: updates propagate over time
- Resilient: if Inventory crashes, Order still succeeds
- Non-blocking: fast response to client

## 🏗️ Implementation in Your Project

### Topic Design

Your project uses **topic-per-aggregate pattern**:

```
├── order-events
│   ├── OrderCreated
│   ├── OrderCancelled
│   └── OrderCompleted
├── payment-events
│   ├── PaymentInitiated
│   ├── PaymentCompleted
│   └── PaymentFailed
├── inventory-events
│   ├── InventoryReserved
│   ├── InventoryReleased
│   └── StockLevelUpdated
└── notification-events
    ├── EmailNotificationSent
    ├── SMSNotificationSent
    └── PushNotificationSent
```

### Producer: Order Service Publishing Events

```java
@Service
public class OrderEventPublisher {
    
    private static final String ORDER_EVENTS_TOPIC = "order-events";
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    /**
     * Publishes OrderCreated event after order is saved
     */
    public void publishOrderCreated(Order order) {
        OrderEvent event = OrderEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("OrderCreated")
            .aggregateId(order.getId())
            .aggregateType("Order")
            .timestamp(LocalDateTime.now())
            .version(1)
            .payload(order)
            .source("order-service")
            .build();
        
        // Key = Order ID ensures all order events go to same partition (preserves order)
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId(), event);
    }
    
    public void publishOrderCancelled(Order order) {
        OrderEvent event = OrderEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("OrderCancelled")
            .aggregateId(order.getId())
            .aggregateType("Order")
            .timestamp(LocalDateTime.now())
            .version(order.getVersion())
            .payload(order)
            .source("order-service")
            .build();
        
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId(), event);
    }
}
```

### Consumer: Inventory Service Listening to Events

```java
@Service
@Slf4j
public class OrderEventListener {
    
    @Autowired
    private InventoryService inventoryService;
    
    /**
     * Listens to order-events topic as part of inventory-service-orders consumer group
     * Consumers in same group partition the work (each partition goes to one consumer)
     */
    @KafkaListener(
        topics = "order-events",
        groupId = "inventory-service-orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received event: {} for order: {}", event.getEventType(), event.getAggregateId());
        
        switch (event.getEventType()) {
            case "OrderCreated":
                handleOrderCreated(event);
                break;
            case "OrderCancelled":
                handleOrderCancelled(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void handleOrderCreated(OrderEvent event) {
        try {
            Order order = (Order) event.getPayload();
            inventoryService.reserveInventory(order.getItems());
        } catch (Exception e) {
            log.error("Failed to reserve inventory for order: {}", event.getAggregateId(), e);
            // Send to dead-letter queue (see Error Handling section)
            throw new RuntimeException(e);
        }
    }
    
    private void handleOrderCancelled(OrderEvent event) {
        Order order = (Order) event.getPayload();
        inventoryService.releaseInventory(order.getItems());
    }
}
```

### Kafka Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    
    # Producer configuration
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all in-sync replicas to acknowledge
      retries: 3
      properties:
        linger.ms: 10  # Batch messages over 10ms for efficiency
        
    # Consumer configuration
    consumer:
      bootstrap-servers: localhost:9092
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.ecommerce.*"
      auto-offset-reset: earliest  # Start from beginning if no offset exists
      max-poll-records: 100  # Batch process up to 100 records
      
    # Consumer group offset management
    listener:
      ack-mode: MANUAL  # Acknowledge after successful processing
      concurrency: 3    # 3 threads per consumer
```

## ⚙️ When to Use Kafka

**Use Kafka when you need:**
- ✅ Decoupling between services
- ✅ Asynchronous processing (fire-and-forget)
- ✅ Audit trail of all events
- ✅ Replay events (event sourcing)
- ✅ Fan-out pattern (one producer → many consumers)
- ✅ Eventual consistency is acceptable

**Don't use Kafka for:**
- ❌ Real-time request-response (use REST/gRPC)
- ❌ Strong consistency requirements (use transactions)
- ❌ Simple caching (use Redis)
- ❌ Immediate synchronous validation

## 🔴 Common Pitfalls

### 1. Losing Message Ordering

**Problem**: Need all Order events for same customer to process sequentially.

**Solution**: Use Order ID as message key
```java
kafkaTemplate.send(topic, orderId, event);  // Same order ID = same partition
```

### 2. Duplicate Message Processing

**Problem**: Consumer crashes after processing but before committing offset.

**Solution**: Make consumers idempotent
```java
@KafkaListener(topics = "order-events", groupId = "orders-group")
public void handleEvent(OrderEvent event) {
    // Check if already processed (using eventId)
    if (repository.existsByEventId(event.getEventId())) {
        return;  // Already processed
    }
    // Process and save with eventId
    processAndSave(event);
}
```

### 3. Consumer Lag Growing

**Problem**: Messages accumulating faster than consumers can process.

**Solution**:
- Increase number of partitions (up to number of consumers)
- Optimize consumer processing time
- Monitor consumer lag: `kafka-consumer-groups --bootstrap-server localhost:9092 --group my-group --describe`

### 4. Dead Messages (Non-Parseable JSON)

**Problem**: Bad JSON prevents entire consumer from starting.

**Solution**: Add error handling with dead-letter topic
```java
@KafkaListener(topics = "order-events", groupId = "orders")
public void handleEvent(OrderEvent event) {
    try {
        process(event);
    } catch (Exception e) {
        sendToDeadLetterQueue(event, e);
    }
}

private void sendToDeadLetterQueue(OrderEvent event, Exception e) {
    kafkaTemplate.send("order-events-dlq", event);
    log.error("Sent to DLQ: {}", event, e);
}
```

## 🔗 Resources

- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- Your Project: See `ADR-002-Kafka-Events-Design.md` for architecture decisions
- Your Project: See `order-service` for producer implementation
- Your Project: See `inventory-service` for consumer implementation

---

**Next**: Read [Microservices Architecture](02-Microservices-Architecture.md) to understand how Kafka fits into the larger system design.

