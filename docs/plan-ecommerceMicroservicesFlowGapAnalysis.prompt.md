# System Flow and Gap Analysis (E-Commerce Microservices)

## 1) Target Flow (Normal E-Commerce Microservices)

### A. Edge and Security Flow
1. Client calls `API Gateway` only (`localhost:9080`).
2. Gateway validates JWT / auth policy.
3. Gateway routes request to internal service (`lb://...` via Eureka).
4. Internal services are not directly exposed publicly.

### B. Core User Story (Happy Path)
1. User registers/logs in and gets JWT.
2. User browses product catalog and stock availability.
3. User creates order.
4. Order service validates customer and reserves inventory.
5. Payment service processes payment.
6. Order state becomes `CONFIRMED/COMPLETED`.
7. Notification service sends email/SMS/push.
8. User can query order status and notification history.

### C. Event-Driven Path
1. `order-service` publishes `OrderCreated`/`OrderCancelled`.
2. `payment-service` publishes `PaymentCompleted`/`PaymentFailed`.
3. `inventory-service` and `notification-service` consume events.
4. DLQ + retry + idempotency protect reliability.

---

## 2) Current Flow in Your Project (Observed)

### What is implemented
- Config + discovery + gateway foundation exists:
  - `config-service/src/main/resources/config/gateway-service.yml`
  - `discovery-service`
  - `gateway-service`
- Main business services exist with APIs:
  - `customer-service`, `order-service`, `inventory-service`, `payment-service`, `notification-service`
- Order/Payment/Notification have clear eventing pieces:
  - Kafka publishers/consumers found in:
    - `order-service/src/main/java/org/ecommerce/orderservice/infrastructure/messaging`
    - `payment-service/src/main/java/org/ecommerce/paymentservice/infrastructure/messaging`
    - `notification-service/src/main/java/org/ecommerce/notificationservice/infrastructure/messaging`
- Notification email via MailDev is integrated (`smtp 1025`):
  - `config-service/src/main/resources/config/notification-service.yml`

### Notable architecture differences
- `customer-service` uses MongoDB, while ADR target describes DB-per-service PostgreSQL model:
  - `customer-service/pom.xml` (`spring-boot-starter-data-mongodb`)
  - ADR reference: `docs/ADR-001-Microservices-Architecture.md`
- Order internal calls are URL-based Feign targets, not service-id load-balanced calls:
  - `config-service/src/main/resources/config/order-service.yml` (`customer-url`, `inventory-url`, `payment-url` with `localhost`)

---

## 3) Gap Analysis (What is Missing)

## 🔴 Critical to “complete” microservice platform

1. **Authentication and Authorization layer is missing**
- No JWT/security stack found in service poms and code (`spring-boot-starter-security`, `@PreAuthorize`, JWT filters not present).
- Impact: all business endpoints are effectively unprotected.

2. **Direct service bypass is still possible in local topology**
- Services are still callable on their own host ports.
- Target state should enforce gateway-only access (network-level or gateway-token policy).

3. **Redis-based patterns are not implemented**
- No Redis starter/RedisTemplate/cache annotations found.
- Missing key learning goals: distributed locks/caching/session tokens.

4. **Testing pyramid is incomplete**
- Unit tests exist, but little/no evidence of EmbeddedKafka coverage and broad Testcontainers usage across services.
- ADR target (70/20/10) is not yet achieved.

---

## 🟡 Important to align with your ADRs and user story

5. **User/account service scope does not yet match security ADR**
- `customer-service` currently appears CRUD-oriented, not auth-oriented (register/login/refresh JWT workflow).

6. **Event choreography is partial across all services**
- Notification and order consume events, but inventory event-consumer flow appears incomplete (no `@KafkaListener` in `inventory-service` currently).

7. **Service-to-service resilience policies need hardening**
- Gateway has circuit-breaker dependency, but clear circuit breaker/retry policy usage across service calls is limited.

8. **CI matrix is not covering all core services**
- `.github/workflows/ci-matrix.yml` currently includes up to `inventory-service`; `order-service`, `payment-service`, `notification-service` are not in `all_services`.

9. **Version alignment drift**
- Mixed Spring Boot/Spring Cloud versions across services (example: `4.1.0/2025.1.2` vs `4.1.1/2025.1.3`) can increase upgrade/debug friction.

---

## 🟢 Optional but valuable for production readiness

10. **Observability expansion**
- Add tracing/correlation IDs and business metrics per use case (order lifecycle, payment latency, notification success rate).

11. **Domain scope expansion**
- Typical e-commerce capabilities still missing (cart, shipment, promo/pricing, refund workflow, admin operations).

12. **Contract governance**
- Add API and event contract tests (consumer-driven contracts and schema validation).

---

## 4) Recommended Completion Roadmap

### Phase 1 (Security + Access Control)
1. Implement JWT auth service flow (register/login/refresh).
2. Add JWT validation filters to gateway and services.
3. Restrict direct service access (network or internal token guard).

### Phase 2 (Reliability + Messaging)
1. Complete event consumers/producers for inventory/payment/order parity.
2. Standardize retry/DLQ/idempotency policy for all consumers.
3. Replace hardcoded inter-service URLs with discovery/load-balanced style where possible.

### Phase 3 (Redis + Performance)
1. Add Redis caching where intended (notification history, hot reads).
2. Add distributed lock strategy where required.
3. Review repositories for N+1 and index usage (entity graphs/projections/pageing).

### Phase 4 (Testing + CI)
1. Add EmbeddedKafka tests for event flows.
2. Add Testcontainers integration tests service-by-service.
3. Expand `.github/workflows/ci-matrix.yml` to include all business services.

### Phase 5 (Observability + Hardening)
1. Add tracing and domain metrics dashboards.
2. Add rate limiting and abuse protection at gateway.
3. Add failure-playbook tests (service down, Kafka lag, DB unavailable).

---

## 5) “Done” Definition for This Project

Project is considered complete when:
1. All business APIs are reachable through gateway and protected by auth.
2. Order -> Inventory -> Payment -> Notification flow works end-to-end (happy + failure paths).
3. Redis, Kafka retries/DLQ, and idempotency are actively used.
4. CI runs tests for all services and quality gates pass.
5. Documentation reflects actual architecture and runbook steps.
