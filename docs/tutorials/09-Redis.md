# Redis: Caching and Distributed Locks

> Master in-memory caching strategies, distributed locking, session management, and Redis data structures to dramatically improve performance.

## 📋 Concepts

### Redis: What and Why?

**Redis** = Remote Dictionary Server
- In-memory key-value store (fast: microseconds)
- Supports multiple data structures: Strings, Lists, Sets, Sorted Sets, Hashes
- **NOT a database replacement** (data can be lost if server crashes)
- **Perfect for**: Cache, sessions, locks, real-time analytics

### Cache Strategies

#### 1. Cache-Aside (Lazy Loading)

```
Request for data:
  ↓
Check Redis ─→ Hit? ──→ Return from Redis ✓ (Fast!)
  │
  └─→ Miss? 
      ↓
      Query Database
      ↓
      Store in Redis
      ↓
      Return to client
```

**Code:**
```java
@Service
public class ProductService {
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    public Product getProduct(Long id) {
        String cacheKey = "product:" + id;
        
        // Try to get from cache first
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Cache hit for product: {}", id);
            return cached;
        }
        
        // Cache miss: fetch from database
        log.info("Cache miss for product: {}, querying DB", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException());
        
        // Store in cache with 1-hour expiration
        redisTemplate.opsForValue()
            .set(cacheKey, product, Duration.ofHours(1));
        
        return product;
    }
}
```

**Pros**: 
- ✅ Simple to implement
- ✅ Only frequently accessed data cached
- ✅ Misses don't cause errors

**Cons**:
- ❌ First request always slow (cold cache)
- ❌ Stale data if database updated

#### 2. Write-Through

```
Update request:
  ↓
Update Database
  ↓
Update Redis (same transaction)
  ↓
Return to client

Consequence: Cache always consistent with DB
```

**Code:**
```java
@Transactional
public Product updateProduct(Long id, ProductUpdateRequest request) {
    String cacheKey = "product:" + id;
    
    // Update database first
    Product product = productRepository.findById(id)
        .orElseThrow();
    product.setName(request.getName());
    product.setPrice(request.getPrice());
    productRepository.save(product);
    
    // Then update cache
    redisTemplate.opsForValue().set(cacheKey, product, Duration.ofHours(1));
    
    return product;
}
```

**Pros**:
- ✅ Cache always consistent
- ✅ No cache misses on read

**Cons**:
- ❌ Every write hits both DB and cache (slower)
- ❌ If cache fails, data inconsistent

### Distributed Locking

**Problem**: Two inventory services trying to reserve same stock
```
Service A: SELECT quantity = 10
Service B: SELECT quantity = 10
Service A: UPDATE quantity = 10 - 5 = 5
Service B: UPDATE quantity = 10 - 3 = 7  ❌ Wrong! Should be 2
```

**Solution: Distributed Lock using Redis**
```
Service A: LOCK product:123 (acquires lock)
Service B: LOCK product:123 (waits for A's lock)
Service A: Read stock, Update stock, UNLOCK
Service B: LOCK product:123 (now gets it)
Service B: Read stock, Update stock, UNLOCK
```

**Code:**
```java
@Service
@Slf4j
public class InventoryService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    public void reserveInventory(Long productId, Integer quantity) {
        String lockKey = "inventory:lock:" + productId;
        
        // Acquire distributed lock with timeout
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, 
                        UUID.randomUUID().toString(),  // Unique token
                        Duration.ofSeconds(5));  // Auto-unlock after 5 seconds
        
        if (!lockAcquired) {
            throw new LockAcquisitionException("Cannot acquire lock for product: " + productId);
        }
        
        try {
            // Now safely update database
            Product product = productRepository.findById(productId)
                .orElseThrow();
            
            if (product.getQuantity() < quantity) {
                throw new OutOfStockException();
            }
            
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);
            
            log.info("Successfully reserved {} units of product {}", quantity, productId);
        } finally {
            // Release lock
            redisTemplate.delete(lockKey);
        }
    }
}
```

### Redis Data Structures

| Structure | Use Case | Example |
|-----------|----------|---------|
| **String** | Cache values, counters | `user:123:email = "john@example.com"` |
| **List** | Queue, recent items | `user:123:recent_orders = [order1, order2, order3]` |
| **Set** | Unique values, memberships | `user:123:roles = {ADMIN, CUSTOMER}` |
| **Sorted Set** | Leaderboards, time-series | `top_sellers = {user1: 100, user2: 95}` |
| **Hash** | Nested objects | `user:123 = {name: John, email: john@ex, age: 30}` |

## 🏗️ Implementation in Your Project

### Inventory Service: Product Stock Cache

```java
@Service
@Slf4j
public class ProductCacheService {
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String LOW_STOCK_KEY = "low_stock_products";
    
    /**
     * Get product with cache-aside strategy
     */
    public Product getProduct(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // Fetch from database (not shown)
        Product product = fetchFromDatabase(id);
        
        // Cache for 30 minutes
        redisTemplate.opsForValue()
            .set(cacheKey, product, Duration.ofMinutes(30));
        
        return product;
    }
    
    /**
     * Invalidate cache on stock update
     */
    public void invalidateProductCache(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.info("Invalidated cache for product: {}", id);
    }
    
    /**
     * Track low-stock products using Sorted Set
     */
    public void trackLowStockProduct(Long productId, Integer quantity) {
        if (quantity < 10) {  // Threshold
            redisTemplate.opsForZSet()
                .add(LOW_STOCK_KEY, productId.toString(), quantity);
        }
    }
    
    /**
     * Get all low-stock products
     */
    public Set<String> getLowStockProducts() {
        return redisTemplate.opsForZSet()
            .range(LOW_STOCK_KEY, 0, -1);
    }
}
```

### Notification Service: Caching Sent Notifications

```java
@Service
public class NotificationCacheService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * Check if notification already sent (idempotency)
     */
    public boolean isNotificationSent(String eventId) {
        String cacheKey = "notification:sent:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }
    
    /**
     * Mark notification as sent
     */
    public void markNotificationAsSent(String eventId) {
        String cacheKey = "notification:sent:" + eventId;
        // TTL: 7 days (in case customer asks if we notified them)
        redisTemplate.opsForValue()
            .set(cacheKey, "true", Duration.ofDays(7));
    }
    
    /**
     * Get recent notifications for user (using List)
     */
    public List<String> getRecentNotifications(String userId, int limit) {
        String cacheKey = "user:" + userId + ":notifications";
        return redisTemplate.opsForList()
            .range(cacheKey, 0, limit - 1);
    }
    
    /**
     * Add notification to recent list
     */
    public void addRecentNotification(String userId, String notificationContent) {
        String cacheKey = "user:" + userId + ":notifications";
        
        // Add to front of list
        redisTemplate.opsForList().leftPush(cacheKey, notificationContent);
        
        // Keep only last 100 notifications
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
        
        // Expire after 30 days
        redisTemplate.expire(cacheKey, Duration.ofDays(30));
    }
}
```

### Spring Cache Annotations (Alternative)

```java
@Service
public class ProductService {
    
    /**
     * @Cacheable: Cache the result for 1 hour
     */
    @Cacheable(value = "products", key = "#id", cacheManager = "cacheManager")
    public Product getProduct(Long id) {
        // This runs only on cache miss
        log.info("Fetching product from DB: {}", id);
        return productRepository.findById(id).orElse(null);
    }
    
    /**
     * @CacheEvict: Remove from cache when updated
     */
    @CacheEvict(value = "products", key = "#id")
    public Product updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id).orElse(null);
        product.setName(request.getName());
        return productRepository.save(product);
    }
    
    /**
     * @Caching: Multiple cache operations
     */
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "product-list", allEntries = true)
    })
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

### Redis Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: rootroot  # From docker-compose.yml
    timeout: 2000ms
    client-type: lettuce
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
    
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour in ms
```

## ⚙️ When to Use Redis

**Use Redis for:**
- ✅ Session management
- ✅ Frequently read, infrequently updated data
- ✅ Real-time counters
- ✅ Distributed locks
- ✅ Rate limiting
- ✅ Pub/Sub messaging

**Don't use Redis for:**
- ❌ Data persistence (use database)
- ❌ Permanent storage
- ❌ Encrypted sensitive data

## 🔴 Common Pitfalls

### 1. Excessive Memory Usage

❌ **Problem**: Caching everything indefinitely
```java
redisTemplate.opsForValue().set(cacheKey, value);  // Never expires!
// Redis fills up, crashes
```

✅ **Solution: Set TTL (Time to Live)**
```java
redisTemplate.opsForValue()
    .set(cacheKey, value, Duration.ofMinutes(5));  // Auto-expires
```

### 2. Cache Stampede

❌ **Problem**: Many requests on same cache miss
```
All timeout at 5:00 PM
→ All hit database simultaneously
→ Database overwhelmed
```

✅ **Solution: Cache lock pattern**
```java
String lockKey = "product:lock:" + id;
if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5))) {
    // Only one request fetches from DB, others wait
    Product p = fetchFromDatabase(id);
    redisTemplate.opsForValue().set("product:" + id, p);
}
```

### 3. Stale Cache

❌ **Problem**: Cache not updated after database change
```
User updates email in database
Email still cached with old value for 1 hour
```

✅ **Solution: Actively invalidate**
```java
@CacheEvict(value = "user", key = "#id")  // Clears cache immediately
public User updateUser(Long id, UserUpdateRequest request) {
    // ...
}
```

## 🔗 Resources

- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Commands](https://redis.io/commands/)
- Your Project: Redis used in `docker-compose.yml` for Inventory and Notification services

---

**Next**: Read [Resilience4j](13-Resilience4j.md) for fault tolerance, or explore [Distributed Systems Patterns](Spring-Cloud-Patterns.md).

