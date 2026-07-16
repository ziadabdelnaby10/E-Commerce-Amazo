# MongoDB: Document Database Design

> Learn document-based data modeling for semi-structured data, when to use NoSQL, and how it differs from relational databases.

## 📋 Concepts

### Document Model vs Relational Model

**Relational (PostgreSQL)**: Normalized, structured
```
users table:       orders table:        order_items table:
[id, email]        [id, user_id]        [id, order_id, product_id, qty]
                   [total]
```
Multiple tables joined together.

**Document (MongoDB)**: Denormalized, nested
```javascript
{
  _id: ObjectId("..."),
  email: "john@example.com",
  orders: [
    {
      id: 1,
      total: 99.99,
      items: [
        { productId: "prod-1", qty: 2, price: 49.99 },
        { productId: "prod-2", qty: 1, price: 50.00 }
      ]
    }
  ]
}
```
Single document with nested arrays.

### BSON Data Format

MongoDB stores data as **BSON** (Binary JSON):
```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439011"),  // Unique identifier (auto)
  name: "Product A",
  price: 99.99,                               // Double (floating point)
  inStock: true,                              // Boolean
  tags: ["electronics", "sale"],              // Array
  description: "A great product",
  metadata: {                                 // Nested object
    manufacturer: "Brand X",
    warrantyYears: 2
  },
  createdAt: ISODate("2026-07-16T10:30:00Z") // Date
}
```

### When to Use MongoDB vs PostgreSQL

| Need | PostgreSQL | MongoDB |
|------|-----------|---------|
| **Structured data** (users, orders) | ✅ Best | ❌ Not ideal |
| **Semi-structured** (logs, events) | ❌ Inefficient | ✅ Perfect |
| **Fast changing schema** | ❌ Migration overhead | ✅ Flexible |
| **Complex joins** | ✅ Optimized | ❌ Not encouraged |
| **ACID transactions** | ✅ Full | ⚠️ Single document |
| **Scaling horizontally** | ❌ Complex | ✅ Sharding built-in |

## 🏗️ Implementation in Your Project

### Use Case: Notification Service Event Archive

Relational approach (limited):
```sql
-- Over-normalized, hard to query
notifications table:
[id, user_id, event_type, email_sent, sms_sent, timestamp]

-- To get all events for a user across different types, complex queries needed
```

Document approach (better fit):
```javascript
db.notifications.insertOne({
  _id: ObjectId(...),
  userId: "user-123",
  timestamp: ISODate("2026-07-16T10:00:00Z"),
  events: [
    {
      eventType: "OrderCreated",
      orderId: "order-456",
      details: { amount: 99.99, items: 2 },
      notificationsSent: {
        email: { sent: true, timestamp: ISODate("2026-07-16T10:00:01Z") },
        sms: { sent: false, reason: "No phone number" }
      }
    },
    {
      eventType: "PaymentCompleted",
      paymentId: "pay-789",
      details: { amount: 99.99 },
      notificationsSent: {
        email: { sent: true, timestamp: ISODate("2026-07-16T10:05:00Z") }
      }
    }
  ]
})
```

### Spring Data MongoDB: Notification Service

```java
// Entity: Stored as BSON document
@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationArchive {
    @Id
    private String id;  // MongoDB auto-generates ObjectId
    
    private String userId;
    
    @CreatedDate
    private LocalDateTime archiveCreatedAt;
    
    private List<NotificationEvent> events = new ArrayList<>();
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;  // "OrderCreated", "PaymentCompleted", etc.
    private String aggregateId;
    private LocalDateTime timestamp;
    private Map<String, Object> details;  // Flexible schema
    private NotificationStatus status;
}

// Repository: Queries on documents
public interface UserNotificationArchiveRepository 
    extends MongoRepository<UserNotificationArchive, String> {
    
    // Find by user ID
    Optional<UserNotificationArchive> findByUserId(String userId);
    
    // Query nested arrays
    @Query("{ 'userId': ?0, 'events.eventType': ?1 }")
    Optional<UserNotificationArchive> findByUserIdAndEventType(
        String userId, 
        String eventType
    );
    
    // Find with time range
    @Query("{ 'events.timestamp': { $gte: ?0, $lte: ?1 } }")
    List<UserNotificationArchive> findEventsInRange(
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
}
```

### Kafka Consumer → MongoDB Storage

```java
@Service
@Slf4j
public class EventArchiveService {
    @Autowired
    private UserNotificationArchiveRepository archiveRepository;
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    /**
     * Listen to all events and archive them
     */
    @KafkaListener(topics = "order-events", groupId = "notification-archive")
    public void archiveOrderEvent(OrderEvent event) {
        UserNotificationArchive archive = archiveRepository
            .findByUserId(event.getUserId())
            .orElse(new UserNotificationArchive());
        
        archive.setUserId(event.getUserId());
        
        NotificationEvent notifEvent = new NotificationEvent(
            event.getEventType(),
            event.getAggregateId(),
            event.getTimestamp(),
            event.getPayload(),  // Flexible: any JSON structure
            NotificationStatus.ARCHIVED
        );
        
        archive.getEvents().add(notifEvent);
        archiveRepository.save(archive);
        
        log.info("Archived event for user: {}", event.getUserId());
    }
}
```

## ⚙️ When to Use MongoDB in Your Project

**For Notification Service:**
- ✅ Semi-structured event data
- ✅ Each event type has different fields
- ✅ Querying by user + event type
- ✅ Archive grows continuously
- ✅ No complex joins needed

**NOT for:**
- ❌ Order Service (structured, relational)
- ❌ User Service (ACID needed)
- ❌ Payment Service (strict data consistency)

## 🔴 Common Pitfalls

### 1. Treating MongoDB Like SQL

❌ **Problem**: Trying to join documents
```javascript
db.orders.aggregate([
  { $lookup: { from: "users", /* complex join logic */ } }
])
// Possible but inefficient - not MongoDB's strength
```

✅ **Solution**: Denormalize (embed related data)
```javascript
{
  _id: ObjectId(...),
  orderId: "order-123",
  user: {  // Embed user data instead of join
    id: "user-1",
    name: "John",
    email: "john@example.com"
  },
  items: [...]
}
```

### 2. No Schema Validation

❌ **Problem**: Inconsistent document structure
```javascript
db.notifications.insertOne({ userId: "user-1", events: [] })
db.notifications.insertOne({ user_id: "user-2" })  // Different field name!
db.notifications.insertOne({ userId: 123 })  // Different type!
```

✅ **Solution**: Use Spring validation
```java
@Document
public class UserNotificationArchive {
    @NotBlank
    private String userId;  // Validated on save
    
    @NotNull
    private List<NotificationEvent> events;
}
```

### 3. Large Documents

❌ **Problem**: Embedding too much data in single document
```javascript
{
  userId: "user-1",
  events: [/* 100,000 notification events */]  // > 16MB limit!
}
// MongoDB document limit: 16MB
```

✅ **Solution**: Archive by time period
```javascript
// Separate collection per month
db.notifications_2026_07.insertOne({ userId: "user-1", events: [...] })
db.notifications_2026_08.insertOne({ userId: "user-1", events: [...] })
```

### 4. No Index Optimization

❌ **Problem**: Querying userId without index
```javascript
db.notifications.find({ userId: "user-1" })  // Full collection scan
```

✅ **Solution: Add indexes**
```java
@Document(collection = "notifications")
@CompoundIndex(name = "user_timestamp_idx", 
               def = "{'userId': 1, 'archiveCreatedAt': -1}")
public class UserNotificationArchive {
    private String userId;
    private LocalDateTime archiveCreatedAt;
}
```

## 🔗 Resources

- [MongoDB Documentation](https://docs.mongodb.com/)
- [Spring Data MongoDB Reference](https://spring.io/projects/spring-data-mongodb)
- [MongoDB University - Free Courses](https://university.mongodb.com/)
- Your Project: Notification Service uses MongoDB (optional)

---

**Next**: Read [Redis](09-Redis.md) for caching strategies, or return to [PostgreSQL](04-PostgreSQL.md) for deeper RDBMS understanding.

