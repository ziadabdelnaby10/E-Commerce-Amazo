# Customer Service

Customer service is now backed by PostgreSQL and Flyway and persists users in the `users` table with UUID identifiers.

## Current scope

- CRUD APIs under `GET/POST/PUT/DELETE /v1/customers`
- UUID-based identity (`users.id`)
- Password hashing via `BCryptPasswordEncoder` (`password_hash` column)
- Default role assignment (`ROLE_USER`) at customer creation
- Internal-only role/permission service (`RolePermissionService`) for RBAC management
- Customer creation fails with conflict when default role `ROLE_USER` is missing
- Soft delete using `deleted_at`
- MapStruct + Lombok based DTO/entity mapping

## Run tests

```bash
./mvnw test
```

## Run locally

```bash
./mvnw spring-boot:run
```

