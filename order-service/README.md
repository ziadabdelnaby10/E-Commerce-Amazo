# Order Service

Order lifecycle service built with a clean architecture style:
- `adapter.in.web`: REST API entry points
- `application`: use cases + orchestration
- `domain`: entities/enums aligned with Flyway schema
- `infrastructure`: persistence, MapStruct mapping, Kafka messaging

## Implemented in this slice

- `POST /api/v1/orders` with `Idempotency-Key` and `X-User-Id`
- `POST /api/v1/orders/{userId}` (legacy compatibility endpoint)
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders?userId=&status=&page=&size=`
- Synchronous calls to inventory reservation and payment initiation with resilience fallback
- Outbox table publishing to Kafka topic `order-events`
- Payment event consumer from `payment-events` with retry + DLQ (`payment-events-dlq`)
- MapStruct mapper for DTO/entity transformations
- JPA query paths optimized for migration indexes (`V1.1__Add_composite_indexes.sql`)

## Run tests

```bash
./mvnw test
```

## Local run

```bash
./mvnw spring-boot:run
```

## Notes

- Service schema is managed by Flyway (`src/main/resources/db/migration`).
- `OrderEvent` acts as outbox; scheduler publishes unpublished rows and marks them as sent.
- Payment events (`PaymentCompleted` / `PaymentFailed`) update order + status history.

