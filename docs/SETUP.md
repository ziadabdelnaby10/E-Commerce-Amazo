# 🚀 Microservices Setup Guide

## Prerequisites Check

```bash
# Check Java
java -version
# Should be: openjdk 21.0.x or similar

# Check Maven
mvn -version
# Should be: 3.8+

# Check Docker
docker --version
docker-compose --version
```

## Step 1: Start Infrastructure (5 minutes)

```bash
# Navigate to project root
cd /path/to/project

# Start all services
docker-compose up -d

# Verify all containers are running
docker-compose ps

# Expected output:
# NAME                  STATUS
# ecommerce-postgres    Up (healthy)
# ecommerce-redis       Up (healthy)
# ecommerce-zookeeper   Up (healthy)
# ecommerce-kafka       Up (healthy)
# ecommerce-kafka-ui    Up
```

## Step 2: Create Each Spring Boot Service

For each service, run the command below. This downloads a pre-configured Spring Boot project template.

### 2.1 User Service (8001)

```bash
curl https://start.spring.io/starter.zip \
  -d artifactId=user-service \
  -d bootVersion=3.4.5 \
  -d dependencies=lombok,configuration-processor,web,data-jpa,postgresql,data-redis,validation,cache,security,testcontainers,kafka \
  -d javaVersion=21 \
  -d packageName=com.ecommerce.user \
  -d packaging=jar \
  -d type=maven-project \
  -o user-service.zip && unzip -o user-service.zip -d . && rm user-service.zip
```

### 2.2 Order Service (8002)

```bash
curl https://start.spring.io/starter.zip \
  -d artifactId=order-service \
  -d bootVersion=3.4.5 \
  -d dependencies=lombok,configuration-processor,web,data-jpa,postgresql,data-redis,validation,cache,security,testcontainers,kafka \
  -d javaVersion=21 \
  -d packageName=com.ecommerce.order \
  -d packaging=jar \
  -d type=maven-project \
  -o order-service.zip && unzip -o order-service.zip -d . && rm order-service.zip
```

### 2.3 Inventory Service (8003)

```bash
curl https://start.spring.io/starter.zip \
  -d artifactId=inventory-service \
  -d bootVersion=3.4.5 \
  -d dependencies=lombok,configuration-processor,web,data-jpa,postgresql,data-redis,validation,cache,security,testcontainers,kafka \
  -d javaVersion=21 \
  -d packageName=com.ecommerce.inventory \
  -d packaging=jar \
  -d type=maven-project \
  -o inventory-service.zip && unzip -o inventory-service.zip -d . && rm inventory-service.zip
```

### 2.4 Payment Service (8004)

```bash
curl https://start.spring.io/starter.zip \
  -d artifactId=payment-service \
  -d bootVersion=3.4.5 \
  -d dependencies=lombok,configuration-processor,web,data-jpa,postgresql,data-redis,validation,cache,security,testcontainers,kafka \
  -d javaVersion=21 \
  -d packageName=com.ecommerce.payment \
  -d packaging=jar \
  -d type=maven-project \
  -o payment-service.zip && unzip -o payment-service.zip -d . && rm payment-service.zip
```

### 2.5 Notification Service (8005)

```bash
curl https://start.spring.io/starter.zip \
  -d artifactId=notification-service \
  -d bootVersion=3.4.5 \
  -d dependencies=lombok,configuration-processor,web,data-jpa,postgresql,data-redis,validation,cache,security,testcontainers,kafka \
  -d javaVersion=21 \
  -d packageName=com.ecommerce.notification \
  -d packaging=jar \
  -d type=maven-project \
  -o notification-service.zip && unzip -o notification-service.zip -d . && rm notification-service.zip
```

## Step 3: Add Additional Dependencies

All services need these additional dependencies. Add to each `pom.xml` in the `<dependencies>` section:

```xml
<!-- SpringDoc OpenAPI (Swagger UI) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>

<!-- Architecture Testing -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.2.1</version>
    <scope>test</scope>
</dependency>

<!-- Resilience4j for Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- REST Assured for API Testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>

<!-- Flyway for Database Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>9.22.3</version>
</dependency>
```

## Step 4: Configure Flyway Database Migrations

### What are Flyway Migrations?

Flyway automatically manages database schema versions. When your service starts:
1. Flyway checks the `db/migration` folder
2. Applies any new migration files in order (by version number)
3. Tracks migrations in database (flyway_schema_history table)
4. Prevents applying the same migration twice

### Migration File Naming Convention

```
V[version]__[description].sql

Examples:
V1.0__User_initial_schema.sql
V1.1__Add_audit_table.sql
V2.0__Refactor_user_roles.sql
```

### Files Already Created

Each service has a migration file ready:
- User Service: `src/main/resources/db/migration/V1.0__User_initial_schema.sql`
  - Tables: users, roles, permissions, user_roles, audit_logs, refresh_tokens, password_reset_tokens
- Order Service: `src/main/resources/db/migration/V1.0__Order_initial_schema.sql`
  - Tables: orders, order_items, order_status_history, order_events, idempotency_keys
- Inventory Service: `src/main/resources/db/migration/V1.0__Inventory_initial_schema.sql`
  - Tables: products, stock_levels, inventory_transactions, inventory_events, distributed_locks, low_stock_alerts
- Payment Service: `src/main/resources/db/migration/V1.0__Payment_initial_schema.sql`
  - Tables: payments, payment_methods, refunds, payment_transactions, fraud_checks, webhook_events
- Notification Service: `src/main/resources/db/migration/V1.0__Notification_initial_schema.sql`
  - Tables: notifications, notification_preferences, notification_templates, notification_events, failed_notifications

### Add Flyway Configuration to application.yml

Each service needs Flyway settings:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    table: flyway_schema_history
```

**When servers start, Flyway will:**
1. Connect to PostgreSQL
2. Create tables from migration files
3. Log all schema changes

### Verify Migrations Applied

After each service starts, check the migration:

```bash
# Connect to PostgreSQL
docker exec -it ecommerce-postgres psql -U postgres -d user_db

# List tables
\dt

# View migration history
SELECT * FROM flyway_schema_history;

# Exit
\q
```

## Step 5: Configure Each Service

Each service gets `src/main/resources/application.yml`. Replace `[SERVICE_NAME]` with actual service name:

```yaml
spring:
  application:
    name: [SERVICE_NAME]
  
  # Flyway Configuration
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate  # IMPORTANT: Flyway handles schema, not Hibernate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  # PostgreSQL Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/[service_name]_db
    username: postgres
    password: rootroot
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
  
  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      password: rootroot
      timeout: 60000
      jedis:
        pool:
          max-active: 8
          max-idle: 8
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
      batch-size: 16384
    consumer:
      bootstrap-servers: localhost:9092
      group-id: [service_name]-consumer-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 100
  
  # Security
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8001  # User Service

# Server Configuration
server:
  port: [PORT]              # 8001, 8002, 8003, 8004, or 8005
  servlet:
    context-path: /api
  compression:
    enabled: true
    min-response-size: 1024

# Logging
logging:
  level:
    root: INFO
    com.ecommerce: DEBUG
    org.hibernate.SQL: DEBUG
    org.flywaydb: INFO  # Monitor migrations
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Swagger/OpenAPI
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
    tags-sorter: alpha
  api-docs:
    path: /api-docs
```

## Step 6: Test Each Service

```bash
# Build User Service
cd user-service
mvn clean install

# Run User Service (in terminal 1)
mvn spring-boot:run

# In a new terminal, test it
curl http://localhost:8001/api/actuator/health

# Check Swagger UI
# Open browser: http://localhost:8001/api/swagger-ui.html

# Verify database tables created
docker exec -it ecommerce-postgres psql -U postgres -d user_db -c "\dt"
```

Repeat for each service in separate terminals:
```bash
cd order-service && mvn spring-boot:run        # Terminal 2, Port 8002
cd inventory-service && mvn spring-boot:run    # Terminal 3, Port 8003  
cd payment-service && mvn spring-boot:run      # Terminal 4, Port 8004
cd notification-service && mvn spring-boot:run # Terminal 5, Port 8005
```

## Step 7: Verify Setup

```bash
# Check all services are running
for port in 8001 8002 8003 8004 8005; do
  echo "Service on port $port:"
  curl -s http://localhost:$port/api/actuator/health | jq '.status'
done

# Expected output:
# Service on port 8001:
# "UP"
# Service on port 8002:
# "UP"
# ... and so on

# Verify all databases and tables created
docker exec ecommerce-postgres psql -U postgres -c "\l"
# Should show: user_db, order_db, inventory_db, payment_db, notification_db

# Check Kafka topics
docker exec ecommerce-kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check Redis
docker exec ecommerce-redis redis-cli -a rootroot ping
# Expected: PONG
```

## Database Schema Overview

### User Service Database
- **users** - User accounts, authentication
- **roles** - User roles (ADMIN, USER, SUPPORT, SYSTEM)
- **permissions** - Fine-grained permissions
- **user_roles** - Mapping users to roles
- **role_permissions** - Mapping roles to permissions
- **refresh_tokens** - JWT refresh tokens
- **audit_logs** - Security event logging
- **password_reset_tokens** - Temporary reset tokens

### Order Service Database
- **orders** - Order records
- **order_items** - Items within orders (1:M relationship)
- **order_status_history** - Track status changes
- **order_events** - Event sourcing for saga pattern
- **idempotency_keys** - Prevent duplicate orders

### Inventory Service Database
- **products** - Product catalog
- **stock_levels** - Current inventory count
- **inventory_transactions** - Audit trail
- **inventory_events** - Event sourcing
- **distributed_locks** - Redis lock tracking
- **low_stock_alerts** - Reorder alerts

### Payment Service Database
- **payments** - Payment records
- **payment_methods** - Saved payment methods (encrypted)
- **payment_transactions** - Individual transactions
- **refunds** - Refund records
- **payment_audit_log** - Compliance logging
- **fraud_checks** - Fraud detection results
- **webhook_events** - Payment gateway callbacks

### Notification Service Database
- **notifications** - Notification records
- **notification_preferences** - User preferences
- **notification_templates** - Email/SMS templates
- **notification_events** - Events from Kafka
- **notification_log** - History
- **failed_notifications** - Dead letter queue
- **email_bounces** - Email bounce tracking
- **sms_delivery_reports** - SMS delivery status

## Troubleshooting

### Services won't start due to database errors

```bash
# Check service logs for Flyway errors
mvn spring-boot:run 2>&1 | grep -i flyway

# Check if Flyway migration succeeded
docker exec ecommerce-postgres psql -U postgres -d [service_name]_db -c \
  "SELECT version, description, success FROM flyway_schema_history;"

# If migration failed, check database directly
docker exec -it ecommerce-postgres psql -U postgres
```

### Ports won't bind
```bash
# Check if ports are in use
lsof -i :8001

# If already in use, kill the process
lsof -ti:8001 | xargs kill -9
```

### Database connection errors
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs ecommerce-postgres

# Test connection
docker exec ecommerce-postgres psql -U postgres -c "SELECT 1"
```

### Kafka connection errors
```bash
# Verify Kafka is running
docker ps | grep kafka

# Check broker status
docker exec ecommerce-kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Check topics
docker exec ecommerce-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Redis connection errors
```bash
# Verify Redis is running
docker ps | grep redis

# Test connection
docker exec ecommerce-redis redis-cli -a rootroot ping
```

## Next Steps

1. ✅ Start infrastructure (`docker-compose up -d`)
2. ✅ Create all 5 services
3. ✅ Add configurations (including Flyway)
4. ✅ Build and run each service
5. 👉 Follow [Learning Path](README.md#-learning-path-4-6-weeks) in README.md

## Quick Reference

| Service | Port | Database | Flyway Migration |
|---------|------|----------|------------------|
| User Service | 8001 | user_db | V1.0__User_initial_schema.sql |
| Order Service | 8002 | order_db | V1.0__Order_initial_schema.sql |
| Inventory Service | 8003 | inventory_db | V1.0__Inventory_initial_schema.sql |
| Payment Service | 8004 | payment_db | V1.0__Payment_initial_schema.sql |
| Notification Service | 8005 | notification_db | V1.0__Notification_initial_schema.sql |

## Cleanup

When done with development:

```bash
# Stop all services
docker-compose down

# Remove volumes (data)
docker-compose down -v

# Stop all running services
kill %1 %2 %3 %4 %5  # In bash/zsh
```
