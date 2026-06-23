# ADR-001: Microservices Architecture

**Date:** 2026-06-22  
**Status:** Accepted  
**Context:** Building a training system to master Spring Boot microservices

## Problem

How should we structure a train system that teaches enterprise-grade backend development with Spring Boot, focusing on JPA, Kafka, Redis, and Security?

## Decision

Implement a **polyglot microservices architecture** with:
- 5 independent Spring Boot services (User, Order, Inventory, Payment, Notification)
- Separate PostgreSQL databases per service (database-per-service pattern)
- Kafka for asynchronous event-driven communication
- Redis for distributed caching and locks
- Docker Compose for local development

## Rationale

- **Learning Value**: Each service teaches different patterns (auth, messaging, caching, transactions)
- **Database Isolation**: Enforces loose coupling; each service owns its schema
- **Event-Driven**: Kafka teaches async patterns, eventual consistency, and saga pattern
- **Scalability**: Demonstrates how services can scale independently
- **Real-World Patterns**: Reflects production microservices architecture

## Database Strategy: Database-Per-Service

### Pattern
```
User Service → user_db
Order Service → order_db
Inventory Service → inventory_db
Payment Service → payment_db
Notification Service → notification_db
```

### Alternatives Considered

1. **Shared Database** ❌
   - Creates tight coupling
   - DB schema changes break multiple services
   - Doesn't teach service isolation

2. **Event Sourcing** ✅ (Learning, not production)
   - Teaches immutability and replay patterns
   - Too complex for initial training
   - Optional: Inventory service uses event sourcing basics

## Communication Strategy

### Service-to-Service (Synchronous)
- Order Service calls Inventory Service REST API
- Order Service calls Payment Service REST API
- Use Circuit Breakers (Resilience4j) for fault tolerance
- Teaching: REST clients, timeout handling, fallbacks

### Service-to-Service (Asynchronous)
- Services publish domain events to Kafka
- Topics: `order.created`, `payment.completed`, `inventory.reserved`, etc.
- Services subscribe to relevant topics
- Teaching: Kafka producer/consumer, event sourcing, saga pattern

## Caching Strategy

### Redis Usage
- **User Sessions**: JWT refresh tokens (TTL: 7 days)
- **Inventory Locks**: Distributed locks for stock updates
- **Query Cache**: Notification service caches recent events
- Teaching: Cache-aside pattern, TTL, distributed locks

### Cache Invalidation
- Event-driven invalidation: When order created, invalidate user's order cache
- Time-based expiration: 5-minute TTL for most caches
- Manual invalidation: Admin operations

## Security Model

### API Gateway (Future Enhancement)
- Centralized authentication
- JWT validation
- Rate limiting
- Request logging

### Per-Service Security
- User Service: JWT generation and validation
- Payment Service: Data encryption (Spring Security Crypto)
- All Services: Spring Security configuration
- Teaching: Spring Security, OAuth2 concepts, encryption

## Event Flow (Happy Path: Create Order)

```
1. Client POST /orders → API Gateway (JWT validation)
2. API Gateway routes to Order Service
3. Order Service (sync) → REST call → Inventory Service
   - Lock stock with Redis distributed lock
   - Reserve inventory
4. Order Service (sync) → REST call → Payment Service
   - Encrypt payment details
   - Process payment
5. Order Service publishes OrderCreated event → Kafka
6. Inventory Service consumes OrderCreated → Updates stock
7. Notification Service consumes OrderCreated → Sends email
8. Response returns to client

If Payment fails (saga pattern):
- Order Service publishes OrderCancelled event
- Inventory Service consumes OrderCancelled → Releases stock
```

## Technology Choices

| Component | Choice | Why |
|-----------|--------|-----|
| **Framework** | Spring Boot 3.4 | Latest LTS, native compilation ready |
| **JPA Provider** | Hibernate | Standard, teaches relationships/queries |
| **Database** | PostgreSQL 17 | Production-grade, complex types |
| **Message Queue** | Kafka 7.7 | Industry standard, teaches streaming |
| **Cache** | Redis 7 | Fast, teaches data structures |
| **Testing** | JUnit 5 + Testcontainers | Modern, real infrastructure in tests |
| **Security** | Spring Security | Standard for Spring apps |
| **API Docs** | SpringDoc OpenAPI | Auto-generates Swagger UI |

## Data Consistency Approach

### Strong Consistency (User Service)
- ACID transactions
- Synchronous updates
- Used for authentication (critical)

### Eventual Consistency (Order/Inventory/Payment)
- Event-driven updates
- Saga pattern for distributed transactions
- Teaching: Distributed systems challenges

## Performance Targets

- Order creation: <200ms (user perceivable)
- Inventory lock: <100ms (Redis efficient)
- Kafka message processing: <1s for 1000 events
- Database query: <50ms (indexed queries)

## Local Development Setup

- Docker Compose for all infrastructure
- Each service runs independently
- Health checks ensure readiness
- Kafka UI for debugging topics

## Monitoring & Observability (Phase 2)

- Actuator endpoints (/health, /metrics)
- Spring Cloud Sleuth for tracing (optional)
- Prometheus metrics (optional)
- Centralized logging (optional)

## Deployment Strategy (Production)

Once trained, deploy to:
- Kubernetes (pod per service)
- Managed PostgreSQL (RDS/Azure Database)
- Managed Kafka (AWS MSK/Azure Event Hub)
- Redis cluster (ElastiCache/Azure Cache)

## Consequences

### Positive
- ✅ Each service teaches specific patterns
- ✅ Database isolation enforces best practices
- ✅ Event-driven teaches async patterns
- ✅ Local Docker setup mirrors production
- ✅ Scales well with complexity

### Negative
- ❌ Complex initial setup
- ❌ Must manage multiple services during development
- ❌ Eventual consistency challenges
- ❌ Debugging distributed issues is harder

## Next Steps

1. Create 5 Spring Boot services following this architecture
2. Implement database-per-service pattern
3. Set up Kafka topics (ADR-002)
4. Implement security model (ADR-003)
5. Define testing strategy (ADR-004)
