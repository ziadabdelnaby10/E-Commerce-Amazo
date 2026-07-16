# PostgreSQL: Relational Database Design

> Master relational database concepts, advanced queries, indexing, and migrations to build a robust data layer for your microservices.

## 📋 Concepts

### Relational vs NoSQL

| Aspect | PostgreSQL (Relational) | MongoDB (NoSQL) |
|--------|---------|---------|
| **Schema** | Predefined, structured | Flexible, document-based |
| **ACID** | Full ACID transactions | At document level |
| **Scaling** | Vertical (bigger server) | Horizontal (sharding) |
| **Relationships** | Joins, foreign keys | Denormalization, embedding |
| **Query Language** | SQL (universal) | MongoDB Query Language |
| **Your Project** | User, Order, Inventory, Payment | Notification history (optional) |

### Normalization (Eliminating Redundancy)

**❌ Denormalized (Data Duplication)**
```
orders table:
┌────────┬──────────┬─────────────────────┬────────────────┐
│ order_id│ user_id  │ user_name           │ user_email      │
├────────┼──────────┼─────────────────────┼────────────────┤
│ 101    │ user-1   │ John Doe            │ john@example   │
│ 102    │ user-1   │ John Doe            │ john@example   │ ← Duplicate
│ 103    │ user-2   │ Jane Smith          │ jane@example   │
```
**Problem**: User changes email → must update 100 rows if they have 100 orders

**✅ Normalized (No Duplication)**
```
users table:
┌────────┬──────────┬──────────────┐
│ user_id│ name     │ email        │
├────────┼──────────┼──────────────┤
│ user-1 │ John Doe │ john@example │

orders table:
┌────────┬──────────┐
│ order_id│ user_id  │  ← Foreign key
├────────┼──────────┤
│ 101    │ user-1   │
│ 102    │ user-1   │
│ 103    │ user-2   │
```
**Benefit**: User changes email → update 1 row in users table

### Primary Keys and Foreign Keys

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key (unique identifier)
    
    private String email;
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key
    
    @ManyToOne
    @JoinColumn(name = "user_id")  // Foreign key referencing users.id
    private User user;
}
```

### Relationships

**One-to-Many** (User has many Orders)
```
Users Table:              Orders Table:
┌────┬──────┐           ┌────┬─────────┬────────┐
│ id │ name │           │ id │ user_id │ total  │
├────┼──────┤    1   ∞  ├────┼─────────┼────────┤
│ 1  │ John │ ──────────│ 1  │ 1       │ $99.99 │
│ 2  │ Jane │           │ 2  │ 1       │ $49.99 │
└────┴──────┘           │ 3  │ 2       │ $199.99│
                        └────┴─────────┴────────┘
```

**Code:**
```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

**Many-to-Many** (Products have many Suppliers, Suppliers supply many Products)
```
Products:          Product_Suppliers:       Suppliers:
┌──┬────────┐     ┌──────────┬─────────┐   ┌──┬──────────┐
│id│ name   │     │product_id│supplier_id│   │id│ name   │
├──┼────────┤     ├──────────┼─────────┤   ├──┼────────┤
│1 │Laptop  │────→│ 1        │ 1       │←──│1 │ Dell   │
│2 │Monitor │──┐  │ 1        │ 2       │   │2 │ HP     │
└──┴────────┘  └→ │ 2        │ 2       │   └──┴────────┘
                  └──────────┴─────────┘
```

## 🏗️ Implementation in Your Project

### User Service: User → Orders Relationship

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;  // Hashed
    
    @Enumerated(EnumType.STRING)
    private UserRole role;  // ADMIN, CUSTOMER
    
    // One user has multiple orders
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Order Service: Order → OrderItems Relationship

```java
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;  // Reference to User Service
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // PENDING, CONFIRMED, SHIPPED, DELIVERED
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    // One order has multiple line items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
```

### Inventory Service: Product Catalog

```java
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer quantity;  // Current stock
    
    @Version  // Optimistic locking: prevents concurrent updates
    private Long version;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Advanced Queries with JPA Repository

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find orders by user
    List<Order> findByUserId(String userId);
    
    // Find orders with status
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);
    
    // Native SQL query
    @Query(value = "SELECT * FROM orders WHERE total_amount > :amount", 
           nativeQuery = true)
    List<Order> findExpensiveOrders(@Param("amount") BigDecimal amount);
    
    // JPQL query with JOIN
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") String userId);
    
    // Projection: only get specific columns
    @Query("SELECT new com.ecommerce.order.dto.OrderSummary(o.id, o.totalAmount, o.status) "
         + "FROM Order o WHERE o.userId = :userId")
    List<OrderSummary> findOrderSummaries(@Param("userId") String userId);
}
```

### Database Migrations with Flyway

```sql
-- V1.0__Initial_Schema.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V1.1__Add_Indexes.sql
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_products_sku ON products(sku);
```

## ⚙️ When to Use PostgreSQL

**Use PostgreSQL when:**
- ✅ Strong data consistency needed (ACID)
- ✅ Complex relationships (foreign keys, joins)
- ✅ Advanced queries (GROUP BY, aggregations)
- ✅ Transactions across tables
- ✅ Data integrity critical (payment, user data)

**Don't use PostgreSQL for:**
- ❌ Massive unstructured data
- ❌ Real-time analytics (use data warehouse)
- ❌ Caching (use Redis)
- ❌ Event store (use Kafka)

## 🔴 Common Pitfalls

### 1. The N+1 Query Problem

❌ **Problem**:
```java
List<Order> orders = orderRepository.findAll();  // 1 query
for (Order order : orders) {
    List<OrderItem> items = order.getItems();    // N more queries!
}
// Total: 1 + N queries
```

✅ **Solution: JOIN FETCH**
```java
@Query("SELECT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();
// Single query with JOIN, loads all items at once
```

### 2. Lazy Loading Outside Transaction

❌ **Problem**:
```java
@Transactional
public Order getOrder(Long id) {
    return orderRepository.findById(id).orElse(null);  // After method, transaction closes
}

Order order = getOrder(1);
order.getItems().size();  // LazyInitializationException!
```

✅ **Solution: EAGER loading or JOIN FETCH**
```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(Long id);
// or
@OneToMany(fetch = FetchType.EAGER)  // Always load
private List<OrderItem> items;
```

### 3. Missing Indexes

❌ **Problem**: Querying by user_id without index
```sql
SELECT * FROM orders WHERE user_id = 1;  -- Full table scan, slow!
```

✅ **Solution: Add index**
```sql
CREATE INDEX idx_orders_user_id ON orders(user_id);
-- Now query uses index lookup, fast!
```

### 4. Concurrent Updates

❌ **Problem**: Two requests update same product quantity simultaneously
```
Thread 1: SELECT quantity = 10
Thread 2: SELECT quantity = 10
Thread 1: UPDATE quantity = 10 - 5 = 5
Thread 2: UPDATE quantity = 10 - 3 = 7  ← Wrong! Should be 2
```

✅ **Solution: Optimistic locking**
```java
@Entity
public class Product {
    @Version  // JPA handles versioning
    private Long version;
}

@Transactional
public void updateQuantity(Long productId, int decrease) {
    Product p = productRepository.findById(productId);
    p.setQuantity(p.getQuantity() - decrease);  // version auto-incremented
    productRepository.save(p);
}
// If another thread modified it, ObjectOptimisticLockingFailureException thrown
```

## 🔗 Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [Hibernate Best Practices](https://hibernate.org/orm/documentation/)
- Your Project: Database schema migrated in `postgres/init.sql`

---

**Next**: Read [MongoDB](08-MongoDB.md) to compare with document databases, or [Redis](09-Redis.md) for caching strategies.

