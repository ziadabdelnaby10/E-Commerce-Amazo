# Notification Service

Event-driven notification microservice with clean-layered packages:
- `api`: REST endpoints and exception handler
- `application`: use cases + ports
- `domain`: entities/enums aligned to Flyway schema
- `infrastructure`: Kafka, persistence, MailDev SMTP, customer client, MapStruct mapper

## Implemented in this slice

- Kafka consumers for `order-events` and `payment-events`
- Deduplication and processing journal in `notification_events`
- Email sending with `JavaMailSender` through MailDev (`localhost:1025`)
- Outbound event publishing to `notification-events`
- Retry scheduler backed by `failed_notifications`
- Inbox and preference APIs:
  - `GET /api/v1/notifications?userId=&type=&status=&page=&size=`
  - `GET /api/v1/notifications/{notificationId}`
  - `GET /api/v1/notifications/preferences/{userId}`
  - `PUT /api/v1/notifications/preferences/{userId}`

## Run tests

```bash
./mvnw test
```

## Local run

```bash
./mvnw spring-boot:run
```

