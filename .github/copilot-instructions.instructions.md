# Copilot Instructions for E-Commerce Microservices

This is a **Spring Boot microservices training project** with 5+ services communicating via REST, Kafka, PostgreSQL, Redis, and MongoDB. This document helps Copilot understand the codebase architecture, build patterns, and conventions.

## Project Overview

- **Type**: Spring Boot microservices learning platform
- **Architecture**: Distributed event-driven system with API Gateway, service discovery, and infrastructure components
- **Build Tool**: Maven (per-service, with `mvnw` wrapper)
- **Java Version**: 21
- **Infrastructure**: Docker Compose (PostgreSQL, Kafka, Redis, MongoDB, Zookeeper, Prometheus, Grafana)

## Currently Scaffolded Services (in CI/CD matrix)

These services have `pom.xml` and `mvnw/mvnw.cmd`:

- `config-service` (port 8888) - Spring Cloud Config Server
- `customer-service` (port 9001) - User/customer management
- `discovery-service` (port 8761) - Eureka service discovery
- `gateway-service` (port 9080) - API Gateway

**Planned services** (directories exist but not yet scaffolded):
- `order-service` - Order creation and saga coordination
- `inventory-service` - Stock management with Redis locks
- `payment-service` - Payment processing with encryption
- `notification-service` - Async event consumer with caching

## Build & Test Commands

### Per-Service Build (from service root directory)

```bash
# Unit tests only
cd [service-name]
./mvnw clean test

# Full verification (unit + integration tests)
./mvnw clean verify

# Build without tests
./mvnw clean package -DskipTests

# Run service locally
./mvnw spring-boot:run

# Single test class
./mvnw test -Dtest=UserControllerTest

# Single test method
./mvnw test -Dtest=UserControllerTest#testCreateUser
```

### Infrastructure

```bash
# Start all infrastructure (from project root)
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker logs ecommerce-postgres
docker logs ecommerce-kafka

# Stop all services
docker-compose down
```

## Key Architectural Patterns

### 1. Service Communication

**Synchronous (REST)**
- Services call each other using RestTemplate or WebClient
- Circuit breaker pattern (Resilience4j) protects against cascading failures
- Inter-service calls from gateway route through discovery service

**Asynchronous (Kafka)**
- Event-driven architecture for order → inventory → payment flows
- Services publish domain events (e.g., `OrderCreated`, `PaymentCompleted`)
- Consumers use `@KafkaListener` with manual offset management
- Topics auto-created via `kafka.auto-create-topics-enable: true`

### 2. Database Strategy

**PostgreSQL** (primary, port 5432 in Docker / 5434 on host)
- User/customer data (user-service, customer-service)
- Order and inventory data
- Schema migrations via Flyway (if needed)
- Connection string: `jdbc:postgresql://localhost:5434/[service_name]_db`
- User: `postgres` / Password: `rootroot`

**MongoDB** (port 27017)
- Optional: audit logs, notifications
- Credentials: root / password

**Redis** (port 6379)
- Distributed locking (inventory stock updates)
- Session/token caching
- Notification history caching
- Password: `rootroot`

### 3. Security Model

- **Spring Security** with JWT tokens in user-service
- **Token validation** across services (decode JWT without calling user-service on every request)
- **Role-based access control (RBAC)** via Spring `@PreAuthorize`
- **Password encryption** with BCrypt
- **Sensitive data** in payment-service encrypted with Spring Security Crypto

### 4. Project Structure (Per Service)

```
[service-name]/
├── pom.xml                           # Maven config, dependencies
├── mvnw / mvnw.cmd                   # Maven wrapper (executable)
├── src/main/java/com/ecommerce/[service]/
│   ├── [ServiceName]Application.java # Spring Boot main class
│   ├── config/                       # Spring config beans
│   ├── controller/                   # REST endpoints (@RestController)
│   ├── service/                      # Business logic
│   ├── repository/                   # JPA repositories (@Repository)
│   ├── entity/                       # JPA entities (@Entity)
│   ├── dto/                          # Data transfer objects (request/response)
│   ├── event/                        # Kafka event classes
│   ├── exception/                    # Custom exceptions, global handler
│   ├── security/                     # Security config (if applicable)
│   └── listener/                     # Kafka consumers (@KafkaListener)
├── src/main/resources/
│   ├── application.yml               # Service-specific config
│   ├── application-test.yml          # Test profile overrides
│   └── db/migration/                 # Flyway migrations (optional)
├── src/test/
│   ├── java/com/ecommerce/[service]/
│   │   ├── controller/               # Controller tests (@WebMvcTest)
│   │   ├── service/                  # Service tests (unit, mocked)
│   │   └── repository/               # Repository tests (@DataJpaTest)
│   └── resources/
│       └── application-test.yml
└── Dockerfile                        # Optional: containerization
```

## Testing Conventions

### Test Profile (`application-test.yml`)

Overrides for tests:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/[service_name]_test_db
  jpa.hibernate.ddl-auto: create-drop
  kafka.bootstrap-servers: localhost:9092
```

### Test Annotations

- `@WebMvcTest(UserController.class)` - Controller tests (no service layer)
- `@DataJpaTest` - Repository tests (real DB, Testcontainers recommended)
- `@SpringBootTest` - Full integration tests (slow, use sparingly)
- `@EmbeddedKafka` - Kafka consumer tests

### Example Unit Test Structure

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;
    
    @Test
    void testCreateUser_Success() {
        // Arrange
        User user = new User("john@example.com", "password");
        when(userRepository.save(any())).thenReturn(user);
        
        // Act
        User result = userService.createUser(user);
        
        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any());
    }
}
```

## Common Configuration Patterns

### application.yml Template

All services follow this structure:

```yaml
spring:
  application.name: [service-name]
  jpa:
    hibernate.ddl-auto: validate
    show-sql: false
  datasource:
    url: jdbc:postgresql://localhost:5434/[service_name]_db
    username: postgres
    password: rootroot
  kafka:
    bootstrap-servers: localhost:9092
    consumer.group-id: [service-name]-group
    producer.value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  redis:
    host: localhost
    port: 6379
    password: rootroot

server:
  port: [PORT_FROM_ports.txt]
```

### Kafka Topic Naming

- `order-created`
- `order-cancelled`
- `payment-completed`
- `payment-failed`
- `inventory-reserved`
- `inventory-released`

Use lowercase with hyphens (kebab-case).

## CI/CD Pipeline

**Workflow file**: `.github/workflows/ci-matrix.yml`

- **Pull Requests**: Runs `./mvnw clean test` on changed services
- **Pushes to main**: Runs `./mvnw clean verify` (includes integration tests)
- **Shared changes** (`.github/`, `docker-compose.yml`, `docs/`, etc.) trigger full matrix

**To add a new service to the matrix**:
1. Create `[service-name]/pom.xml`
2. Add `[service-name]/mvnw` and `[service-name]/mvnw.cmd` (copy from existing service)
3. Add service name to the `all_services` array in `.github/workflows/ci-matrix.yml` (lines 35–40)
4. Ensure `mvn test` passes locally

## Development Workflow

### Starting a New Service

1. Copy an existing service as template (e.g., `cp -r config-service new-service`)
2. Update `pom.xml`: change `artifactId`, `name`, port in `application.yml`
3. Rename main class (e.g., `ConfigServiceApplication` → `NewServiceApplication`)
4. Update package names (e.g., `com.ecommerce.config` → `com.ecommerce.newservice`)
5. Run `./mvnw clean test` to verify setup

### Adding a Dependency

Edit `pom.xml` under `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Then rebuild: `./mvnw clean install`

### Database Migrations

If using Flyway:
- Create migrations in `src/main/resources/db/migration/V1__Init.sql`
- Use `V[number]__[Description].sql` naming

## Key Dependencies & Versions

- **Spring Boot**: 4.1.0 (parent POM)
- **Spring Cloud**: 2025.1.2 (for config, discovery)
- **Java**: 21
- **PostgreSQL driver**: included in spring-boot-starter-data-jpa
- **Kafka**: spring-kafka (in specific services)
- **Redis**: spring-boot-starter-data-redis (in specific services)

## Debugging & Troubleshooting

### Service won't start

```bash
# Check if port is in use
netstat -ano | findstr :8888  # Windows
lsof -i :8888                 # macOS/Linux

# Check application.yml for typos
# Verify PostgreSQL/Kafka are running
docker-compose ps
```

### Integration tests fail

- Ensure `docker-compose up -d` is running
- Check that test profile (`application-test.yml`) has correct DB URL
- Verify Testcontainers can access Docker daemon

### Kafka connection issues

```bash
# Verify Kafka is healthy
docker-compose ps | grep kafka

# Check consumer groups
docker exec ecommerce-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

## Code Review & Commit Conventions

- **Commit messages**: Use conventional commits (feat:, fix:, refactor:, test:)
- **Branch naming**: feature/*, fix/*, refactor/* (kebab-case)
- **PR description**: Include what was changed and why, not just what code changed
- **Test coverage**: Aim for 70%+ unit tests, 20%+ integration tests

## Useful Resources in This Repo

- `docs/API_DESIGN.md` - REST API conventions
- `docs/ADR-*.md` - Architecture decision records
- `postgres/init.sql` - Database schema initialization
- `infra/prometheus/prometheus.yml` - Metrics config
- `README.md` - Full learning path and troubleshooting

## When to Ask for Clarification

- If a service is not yet in the CI matrix but should be
- If new infrastructure (e.g., another database) needs to be added to docker-compose.yml
- If the project shifts from microservices to a different architecture
- If new Spring Boot or Java versions require migration
