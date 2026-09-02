# Payment Service

Payment workflow service built with a clean architecture style:
- `controller`: REST API entry points
- `service`: use-case orchestration
- `domain`: entities/enums aligned with Flyway schema
- `infrastructure`: persistence, MapStruct mapping, Kafka publishing

## Implemented in this slice

- `POST /api/v1/payments` initiate payment for an order
- `GET /api/v1/payments/{paymentId}` retrieve payment with transactions
- `GET /api/v1/payments?userId=&status=&page=&size=` list payments by user
- Payment audit log row created for each initiation
- Kafka publish to `payment-events` with `PaymentCompleted`/`PaymentFailed`
- Query paths aligned with composite indexes (`V1.1__Add_composite_indexes.sql`)

## Run tests

```bash
./mvnw test
```

## Local run

```bash
./mvnw spring-boot:run
```

