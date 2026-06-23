# E-Commerce Microservices Training System

> A comprehensive Spring Boot microservices training project focusing on JPA, PostgreSQL, Kafka, Redis, Security, and Testing.

## 🎯 Learning Objectives

This project trains you on enterprise-grade Spring Boot patterns:

- **JPA/Hibernate**: Entity relationships, repositories, complex queries, performance optimization
- **PostgreSQL**: Database design, migrations, transactions, connection pooling
- **Kafka**: Event-driven architecture, async messaging, saga pattern
- **Redis**: Caching strategies, distributed locks, session management
- **Spring Security**: JWT authentication, OAuth2, role-based access control, encryption
- **Testing**: Unit tests, integration tests, contract testing, Testcontainers
- **Microservices Patterns**: Service communication, fault tolerance, observability

## 📦 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│              (Route, Auth, Rate Limiting)                   │
└────────┬──────────────┬──────────────┬──────────────┬────────┘
         │              │              │              │
    ┌────▼───┐  ┌──────▼───┐  ┌──────▼─────┐  ┌────▼────┐
    │ User   │  │ Order    │  │ Inventory  │  │ Payment │
    │Service │  │ Service  │  │ Service    │  │ Service │
    └────┬───┘  └──────┬───┘  └──────┬─────┘  └────┬────┘
         │             │             │             │
         └─────────────┼─────────────┼─────────────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
    ┌─────▼──┐  ┌─────▼──┐  ┌────▼──────┐
    │ Redis  │  │ Kafka  │  │ PostgreSQL │
    │ Cache  │  │ Events │  │ Databases  │
    └────────┘  └────────┘  └────────────┘
```

### Microservices

1. **User Service** (Port 8001)
   - User registration & authentication
   - JWT token generation
   - Role-based access control
   - Learning: Spring Security, JPA relationships

2. **Order Service** (Port 8002)
   - Order creation and management
   - Calls Inventory & Payment services
   - Publishes OrderCreated/OrderCancelled events
   - Learning: REST client, Kafka producer, saga pattern

3. **Inventory Service** (Port 8003)
   - Product catalog management
   - Stock tracking with distributed locks (Redis)
   - Consumes InventoryReserved events
   - Learning: Kafka consumer, distributed transactions

4. **Payment Service** (Port 8004)
   - Payment processing
   - PCI compliance basics (encrypted storage)
   - Publishes PaymentCompleted/PaymentFailed events
   - Learning: Spring Security encryption, external API integration

5. **Notification Service** (Port 8005)
   - Async event processor
   - Consumes all order/payment events
   - Caches notification history in Redis
   - Learning: Kafka consumer, Redis caching

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- Git

### 1. Start Infrastructure

```bash
cd /path/to/project
docker-compose up -d

# Verify services are healthy
docker-compose ps

# View Kafka topics (optional)
# http://localhost:8080 (Kafka UI)
```

### 2. Create Each Service

For each service, run the setup script (provided below):

```bash
# Create user-service
./create-service.sh user-service 8001

# Create order-service
./create-service.sh order-service 8002

# ... and so on
```

### 3. Build & Run Services

```bash
cd user-service
mvn clean install
mvn spring-boot:run

# In separate terminals:
cd ../order-service
mvn clean install
mvn spring-boot:run

# Repeat for other services
```

### 4. Verify Setup

```bash
# Check User Service
curl http://localhost:8001/actuator/health

# Check Order Service
curl http://localhost:8002/actuator/health

# Check Swagger UI
# http://localhost:8001/swagger-ui.html
```

## 📁 Project Structure

```
.
├── docker-compose.yml          # Infrastructure definitions
├── postgres/
│   └── init.sql               # Database initialization
├── docs/
│   ├── ADR-001-*.md           # Architecture Decision Records
│   └── API_DESIGN.md          # API design patterns
├── user-service/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ecommerce/user/
│   │   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── controller/       # REST endpoints
│   │   │   │   ├── service/          # Business logic
│   │   │   │   ├── repository/       # JPA repositories
│   │   │   │   ├── entity/           # JPA entities
│   │   │   │   ├── dto/              # Data transfer objects
│   │   │   │   └── security/         # Security config
│   │   │   └── resources/
│   │   │       └── application.yml   # Service config
│   │   └── test/
│   │       ├── java/com/ecommerce/user/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   └── repository/
│   │       └── resources/
│   │           └── application-test.yml
│   └── Dockerfile             # Optional: containerize service
├── order-service/             # (Same structure)
├── inventory-service/         # (Same structure)
├── payment-service/           # (Same structure)
└── notification-service/      # (Same structure)
```

## 🔑 Key Learning Topics by Service

### User Service
- [ ] Spring Security configuration
- [ ] JWT token generation and validation
- [ ] Password hashing (BCrypt)
- [ ] One-to-many JPA relationships (@OneToMany, @ManyToOne)
- [ ] Custom authentication provider
- [ ] Role-based access control (RBAC)

### Order Service
- [ ] REST client with RestTemplate/WebClient
- [ ] Kafka producer configuration
- [ ] Saga pattern for distributed transactions
- [ ] Exception handling and circuit breaker (Resilience4j)
- [ ] Request/response logging
- [ ] Transaction management

### Inventory Service
- [ ] Kafka consumer group configuration
- [ ] Distributed locking with Redis
- [ ] Optimistic locking (JPA version field)
- [ ] Complex JPA queries (projections, specifications)
- [ ] Database migrations (Flyway)
- [ ] Event sourcing basics

### Payment Service
- [ ] Data encryption (Spring Security Crypto)
- [ ] Integration with external API (mock payment gateway)
- [ ] Idempotency patterns
- [ ] Audit logging
- [ ] PCI compliance basics
- [ ] Kafka event publishing

### Notification Service
- [ ] Kafka consumer with error handling
- [ ] Caching strategies (Cache-aside, Write-around)
- [ ] Redis operations (SET, GET, TTL)
- [ ] Dead-letter queues
- [ ] Batch processing
- [ ] Monitoring and metrics

## 🧪 Testing Strategy

Each service includes:

1. **Unit Tests** (70% coverage)
   - Mocked dependencies
   - Single responsibility focus
   - Use: JUnit 5, Mockito

2. **Integration Tests** (20%)
   - Real database (Testcontainers)
   - Real Kafka (EmbeddedKafka)
   - Use: @SpringBootTest, @DataJpaTest

3. **Contract Tests** (10%)
   - Service-to-service contracts
   - Use: Pact, REST Assured

4. **End-to-End Tests** (Docker Compose environment)
   - Full system workflow
   - Load testing basics

## 📊 Key Configuration Files

### application.yml
Each service has:
```yaml
spring:
  jpa:
    hibernate.ddl-auto: validate      # Production-ready
    show-sql: false
  kafka:
    bootstrap-servers: localhost:9092
  redis:
    host: localhost
    port: 6379
  datasource:
    url: jdbc:postgresql://localhost:5432/[service_db]
```

## 🔒 Security Considerations

- All services behind API Gateway (planned)
- JWT tokens with short expiry (15 min)
- Redis stores refresh tokens
- Payment service encrypts sensitive data
- All inter-service calls use circuit breakers
- Audit logging for security events

## 📈 Performance Benchmarks

Target metrics for your learning:
- Order creation: <200ms
- Inventory reservation: <100ms (with Redis lock)
- User authentication: <50ms (with JWT cache)
- Kafka processing: <1s for 1000 events

## 🛠 Development Workflow

1. **Per-Service Development**
   ```bash
   cd [service-name]
   mvn spring-boot:run
   ```

2. **Run Tests**
   ```bash
   mvn test                    # Unit tests
   mvn verify                  # With integration tests
   ```

3. **View Logs**
   ```bash
   docker logs ecommerce-postgres
   docker logs ecommerce-kafka
   docker logs ecommerce-redis
   ```

4. **Database Access**
   ```bash
   # Connect to PostgreSQL
   psql -h localhost -U postgres -d user_db
   ```

## 🗺️ Learning Path (4-6 weeks)

### Week 1: Foundations
- [ ] Set up User Service (Spring Security basics)
- [ ] Create User entity and repository
- [ ] Implement JWT authentication
- [ ] Write unit tests

### Week 2: Data Layer
- [ ] Design Order and OrderItem entities (One-to-many)
- [ ] Implement JPA repositories with custom queries
- [ ] Learn N+1 query problem and solutions
- [ ] Write integration tests with Testcontainers

### Week 3: Messaging
- [ ] Set up Kafka topics
- [ ] Implement Order Service Kafka producer
- [ ] Implement Inventory Service Kafka consumer
- [ ] Learn saga pattern for distributed transactions

### Week 4: Caching & State
- [ ] Add Redis to Inventory Service (distributed locking)
- [ ] Implement caching in Notification Service
- [ ] Learn Redis data structures (ZSET, HSET, etc.)
- [ ] Implement cache invalidation strategies

### Week 5: Advanced Patterns
- [ ] Payment Service encryption
- [ ] Circuit breaker (Resilience4j)
- [ ] API Gateway basics
- [ ] Health checks and monitoring

### Week 6: Production Readiness
- [ ] Comprehensive testing (unit, integration, E2E)
- [ ] Performance optimization
- [ ] Documentation and deployment
- [ ] Capstone: Full order-to-delivery flow

## 🔗 Useful Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Documentation](https://redis.io/docs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

## 🐛 Troubleshooting

### Services won't start
```bash
# Check if ports are already in use
lsof -i :8001
lsof -i :5432

# Check Docker logs
docker logs [container-name]

# Restart infrastructure
docker-compose restart
```

### Kafka connection issues
```bash
# Verify Kafka is running
docker exec ecommerce-kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Check consumer groups
docker exec ecommerce-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

### Database connection issues
```bash
# Check PostgreSQL logs
docker logs ecommerce-postgres

# Verify connection string in application.yml
# Default: jdbc:postgresql://localhost:5432/[service_name]_db
```

## 📝 Contributing

As you complete each service, commit with messages like:
```bash
git commit -m "feat: user-service authentication with JWT"
git commit -m "feat: order-service Kafka producer integration"
git commit -m "refactor: inventory-service distributed locking"
```

## 📄 License

Educational project - MIT License

---

**Next Steps:**
1. Run `docker-compose up -d` to start infrastructure
2. Create each service using Spring Initializr
3. Follow the week-by-week learning path
4. Commit progress to Git
5. Compare your solutions with feedback
