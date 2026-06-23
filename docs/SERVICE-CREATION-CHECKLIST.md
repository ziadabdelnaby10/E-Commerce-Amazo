# Service Creation Checklist

## For Each Service: User, Order, Inventory, Payment, Notification

### Phase 1: Scaffolding ✅
- [ ] Download from start.spring.io (follow SETUP.md)
- [ ] Add additional dependencies (SpringDoc, Resilience4j, REST Assured)
- [ ] Create `application.yml` configuration
- [ ] Create `.gitignore`
- [ ] Initialize Git: `git init`

### Phase 2: Package Structure
```
src/main/java/com/ecommerce/[service]/
├── config/              # Spring configurations
│   ├── SecurityConfig
│   ├── KafkaConfig
│   └── RedisConfig
├── controller/          # REST endpoints (@RestController)
├── service/             # Business logic (@Service)
├── repository/          # Data access (@Repository)
├── entity/              # JPA entities (@Entity)
├── dto/                 # Data transfer objects
├── exception/           # Custom exceptions
├── event/               # Domain events
├── security/            # Security-related classes
└── [ServiceName]Application.java  # Main class
```

### Phase 3: Entity Creation
- [ ] Create JPA entities for domain model
- [ ] Add Hibernate annotations (@Entity, @Table, @Column)
- [ ] Define relationships (@OneToMany, @ManyToOne, etc.)
- [ ] Add Lombok (@Data, @AllArgsConstructor, etc.)
- [ ] Create constructors and methods
- [ ] Add Javadoc comments

### Phase 4: JPA Repository
- [ ] Create Spring Data JPA repository (extend `JpaRepository<Entity, ID>`)
- [ ] Add custom query methods using JPQL or native SQL
- [ ] Create specifications for complex queries
- [ ] Write unit tests for repository layer
- [ ] Test with Testcontainers (real database)

### Phase 5: Service Layer
- [ ] Create service class with business logic
- [ ] Implement CRUD operations
- [ ] Add validation logic
- [ ] Implement caching (if applicable)
- [ ] Handle exceptions gracefully
- [ ] Write unit tests with Mockito
- [ ] Write integration tests

### Phase 6: Controller/Endpoints
- [ ] Create REST controller (@RestController)
- [ ] Implement CRUD endpoints (GET, POST, PUT, DELETE)
- [ ] Add request/response DTOs
- [ ] Add proper HTTP status codes
- [ ] Add error handling (@ExceptionHandler)
- [ ] Add Swagger documentation (@Operation, @ApiResponse)
- [ ] Write integration tests with MockMvc

### Phase 7: Security (User Service focus)
- [ ] Configure Spring Security
- [ ] Implement authentication (if applicable)
- [ ] Add authorization rules (@PreAuthorize)
- [ ] Implement JWT (User Service)
- [ ] Add CORS configuration
- [ ] Test security endpoints

### Phase 8: Kafka Integration
- [ ] Create Kafka producer class (if service publishes events)
- [ ] Configure KafkaTemplate
- [ ] Implement event publishing
- [ ] Create Kafka consumer class (if service consumes events)
- [ ] Configure @KafkaListener
- [ ] Add error handling and retries
- [ ] Write consumer tests with EmbeddedKafka

### Phase 9: Redis Integration
- [ ] Configure Redis connection (RedisTemplate, StringRedisTemplate)
- [ ] Implement caching strategy
- [ ] Add @Cacheable, @CacheEvict annotations (if using Spring Cache)
- [ ] Implement distributed locks (if needed)
- [ ] Test cache behavior

### Phase 10: Testing
- [ ] Unit tests for service (70%)
- [ ] Integration tests with Testcontainers (20%)
- [ ] Controller tests with MockMvc (10%)
- [ ] Kafka tests with EmbeddedKafka
- [ ] Coverage report (aim for 70%+)
- [ ] Run: `mvn clean verify`

### Phase 11: Configuration & Properties
- [ ] Create `application.yml` with all configurations
- [ ] Create `application-test.yml` for test profile
- [ ] Add environment variables for secrets
- [ ] Document all properties in README

### Phase 12: Documentation
- [ ] Add Javadoc to all public methods
- [ ] Create service README (setup, running, testing)
- [ ] Document API endpoints (auto-generated Swagger)
- [ ] Add architecture notes
- [ ] Document database schema

### Phase 13: Build & Deploy Readiness
- [ ] Build successfully: `mvn clean install`
- [ ] All tests pass: `mvn test`
- [ ] No compilation warnings
- [ ] Run linting/formatting checks
- [ ] Package JAR: `mvn clean package`
- [ ] Test JAR runs: `java -jar target/service.jar`

### Phase 14: Git Commit
```bash
git add .
git commit -m "feat: [service-name] initial setup with JPA, security, Kafka"
```

## Service-Specific Checklists

### User Service (Port 8001)
- [ ] User entity with roles/permissions
- [ ] JWT token generation and validation
- [ ] Password hashing with BCrypt
- [ ] One-to-many relationship (User -> Orders)
- [ ] Authentication endpoints (/register, /login, /refresh)
- [ ] Role-based access control
- [ ] Audit logging for security events

### Order Service (Port 8002)
- [ ] Order entity with OrderItem relationship
- [ ] OrderStatus enum (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- [ ] Saga pattern for creating order -> reserve inventory -> process payment
- [ ] Kafka producer (publishes OrderCreated, OrderCancelled events)
- [ ] REST client to call Inventory Service
- [ ] REST client to call Payment Service
- [ ] Circuit breaker for external service calls
- [ ] Idempotency key for request deduplication

### Inventory Service (Port 8003)
- [ ] Product entity with quantity field
- [ ] Kafka consumer for OrderCreated events
- [ ] Distributed lock with Redis for stock updates
- [ ] Optimistic locking with @Version
- [ ] Inventory reservation logic
- [ ] Stock level update events (published to Kafka)
- [ ] Complex query for low-stock products
- [ ] Event sourcing basics (audit table)

### Payment Service (Port 8004)
- [ ] Payment entity with status (PENDING, COMPLETED, FAILED)
- [ ] Encryption of credit card details (Spring Security Crypto)
- [ ] Integration with external payment API (mock)
- [ ] Kafka producer (publishes PaymentCompleted, PaymentFailed events)
- [ ] Idempotency for duplicate requests
- [ ] Audit logging for compliance
- [ ] Webhook handling for payment updates

### Notification Service (Port 8005)
- [ ] Kafka consumer for all event topics
- [ ] Email notification logic (mock)
- [ ] SMS notification logic (mock)
- [ ] Redis cache for notification history
- [ ] Dead-letter queue handling
- [ ] Batch processing of notifications
- [ ] Retry mechanism for failed notifications

## Testing Checklist (Per Service)

- [ ] Unit tests: 70%+ coverage
  - [ ] Service layer (business logic)
  - [ ] Repository layer (queries)
  - [ ] Controller endpoints
  - [ ] Exception handling

- [ ] Integration tests: 20%
  - [ ] Database tests with Testcontainers
  - [ ] Kafka tests with EmbeddedKafka
  - [ ] Redis tests with TestContainers
  - [ ] Full context tests with @SpringBootTest

- [ ] API tests: 10%
  - [ ] Controller tests with MockMvc
  - [ ] REST Assured for API testing
  - [ ] Error response validation

## Performance Checklist

- [ ] Database query optimization (indexed, no N+1)
- [ ] Connection pooling configured
- [ ] Caching strategy implemented
- [ ] Kafka batch processing
- [ ] Load test with JMeter (basic)

## Security Checklist

- [ ] Spring Security configured
- [ ] HTTPS ready configuration
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (JSON escaping)
- [ ] CSRF protection enabled
- [ ] Secrets not hardcoded
- [ ] Audit logging implemented

## Deployment Checklist

- [ ] Dockerfile created
- [ ] Multi-stage build (dev, test, prod)
- [ ] Environment variables configured
- [ ] Health checks configured (/actuator/health)
- [ ] Metrics endpoint enabled (/actuator/metrics)
- [ ] Graceful shutdown configured

## Completion

When all phases complete, service is production-ready! 🚀
