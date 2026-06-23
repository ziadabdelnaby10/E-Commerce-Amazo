# ADR-002: Kafka Event-Driven Architecture

**Date:** 2026-06-22  
**Status:** Accepted  
**References:** ADR-001

## Problem

How should services communicate asynchronously using Kafka to teach event-driven patterns?

## Decision

Implement **Event-Driven Architecture using Kafka** with:
- Topic-per-aggregates pattern (topics for Order, Payment, Inventory, User events)
- Event sourcing basics in Inventory Service
- Consumer groups for scalability
- Dead-letter queues (DLQ) for error handling

## Event Architecture

### Topics & Events

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

### Event Schema (JSON)

```json
{
  "eventId": "uuid-v4",
  "eventType": "OrderCreated",
  "aggregateId": "order-123",
  "aggregateType": "Order",
  "timestamp": "2026-06-22T10:30:00Z",
  "version": 1,
  "payload": {
    "orderId": "order-123",
    "customerId": "user-456",
    "items": [...],
    "totalAmount": 99.99,
    "status": "PENDING"
  },
  "source": "order-service"
}
```

### Consumer Groups

```
order-events topic:
  - order-service (consumer group: order-service-orders)
  - inventory-service (consumer group: inventory-service-orders)
  - notification-service (consumer group: notification-service-orders)
  - payment-service (consumer group: payment-service-orders)

payment-events topic:
  - order-service (consumer group: order-service-payments)
  - notification-service (consumer group: notification-service-payments)
  - inventory-service (consumer group: inventory-service-payments)
```

## Implementation Pattern

### Producer (Order Service)

```java
@Service
public class OrderEventPublisher {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(
            UUID.randomUUID(),
            "OrderCreated",
            order.getId(),
            LocalDateTime.now(),
            order
        );
        kafkaTemplate.send("order-events", order.getId(), event);
    }
}
```

### Consumer (Inventory Service)

```java
@Service
public class OrderEventListener {
    @KafkaListener(topics = "order-events", groupId = "inventory-service-orders")
    public void handleOrderCreated(OrderEvent event) {
        if ("OrderCreated".equals(event.getEventType())) {
            // Reserve inventory
            reserveInventory(event.getPayload());
        }
    }
}
```

## Error Handling Strategy

### Dead-Letter Queue (DLQ)

```
order-events → [Error] → order-events-dlq
```

### Retry Policy

```yaml
spring:
  kafka:
    retry:
      max-attempts: 3
      backoff-delay: 5000  # 5 seconds
      multiplier: 2.0      # Exponential backoff
    listener:
      ack-mode: MANUAL
```

## Saga Pattern Example (Order Create → Payment)

### Happy Path
```
1. Order Service: Create Order (PENDING)
   ↓
2. Order Service: emit OrderCreated event
   ↓
3. Payment Service: consume OrderCreated
   ↓
4. Payment Service: process payment
   ↓
5. Payment Service: emit PaymentCompleted event
   ↓
6. Order Service: consume PaymentCompleted
   ↓
7. Order Service: CONFIRM order (COMPLETED)
```

### Failure Path (Saga Rollback)
```
1. Order Service: Create Order (PENDING)
2. Payment Service: consume OrderCreated → Payment fails
3. Payment Service: emit PaymentFailed event
4. Order Service: consume PaymentFailed
5. Order Service: cancel order (CANCELLED)
6. Inventory Service: consume OrderCancelled → Release stock
```

## Teaching Goals

- [x] Kafka topic design
- [x] Producer/consumer patterns
- [x] Consumer groups and partitioning
- [x] Event schema design
- [x] Error handling and retries
- [x] Saga pattern for distributed transactions
- [x] Idempotent message processing
- [x] Event ordering (partition keys)

## Configuration

### Topics to Create

```bash
# Create topics (auto-created by Kafka if enabled)
kafka-topics --create \
  --topic order-events \
  --partitions 3 \
  --replication-factor 1

kafka-topics --create \
  --topic order-events-dlq \
  --partitions 1 \
  --replication-factor 1
```

### Consumer Group Monitoring

```bash
# List consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check group details
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group inventory-service-orders \
  --describe

# Reset offset (for testing)
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group inventory-service-orders \
  --reset-offsets --to-earliest --execute --all-topics
```

## Next Steps

1. Implement KafkaTemplate in Order Service
2. Configure @KafkaListener in consumer services
3. Implement error handling with DLQ
4. Test saga pattern in integration tests
5. Monitor topics with Kafka UI (http://localhost:8080)
