# Database Schema Guide

> Complete reference for all tables and relationships in the e-commerce microservices system.

## Overview

Each microservice has its own PostgreSQL database with Flyway migrations. Databases are created automatically on startup:
- User Service → `user_db`
- Order Service → `order_db`
- Inventory Service → `inventory_db`
- Payment Service → `payment_db`
- Notification Service → `notification_db`

## User Service Database (`user_db`)

### Entities & Relationships

```
User (1) ──────────── (M) Refresh Tokens
 │
 ├─────────────────── (M) User Roles
 │                         │
 │                         └─ Role (M) ──────── (M) Permissions
 │
├─────────────────── (M) Audit Logs
│
└─────────────────── (1) Password Reset Tokens
```

### Tables

#### 1. **users**
Core user entity for authentication

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,          -- Login identifier
    password_hash VARCHAR(255) NOT NULL,         -- Bcrypt hash
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT true,              -- Soft delete flag
    is_email_verified BOOLEAN DEFAULT false,
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,                        -- Soft delete
    version BIGINT DEFAULT 0                     -- Optimistic locking
);
```

**Key Indexes:**
- `(email)` - Fast login lookups
- `(is_active)` - Filter active users
- `(created_at DESC)` - Recent users query

**Learning Points:**
- Soft deletion pattern (deleted_at instead of hard delete)
- Optimistic locking with version field
- Password hashing (never store plain text)
- Email verification workflow

#### 2. **roles**
Role definitions (reference data)

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,           -- ROLE_ADMIN, ROLE_USER, etc.
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pre-loaded data
INSERT INTO roles (name, description) VALUES 
    ('ROLE_ADMIN', 'Administrator'),
    ('ROLE_USER', 'Regular users'),
    ('ROLE_SUPPORT', 'Support staff'),
    ('ROLE_SYSTEM', 'Internal services');
```

#### 3. **permissions**
Fine-grained access control

```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,          -- VIEW_ORDERS, CREATE_ORDER, etc.
    description VARCHAR(255),
    category VARCHAR(50),                       -- 'orders', 'payments', 'admin'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Sample Permissions:**
```
orders: VIEW_ORDERS, CREATE_ORDER, CANCEL_ORDER
payments: VIEW_PAYMENTS, PROCESS_PAYMENT, REFUND_PAYMENT
admin: VIEW_USERS, CREATE_USER, MODIFY_USER, DELETE_USER
inventory: VIEW_INVENTORY, MODIFY_INVENTORY
```

#### 4. **user_roles** (Many-to-Many)
Maps users to roles (RBAC)

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,                         -- Admin who assigned role
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

**Learning Points:**
- Composite primary key for M:M relationships
- Cascade delete
- Audit trail (assigned_by, assigned_at)

#### 5. **role_permissions** (Many-to-Many)
Maps roles to permissions

```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
```

#### 6. **refresh_tokens**
JWT refresh token tracking (stored in Redis, audited in DB)

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,   -- Hash of JWT
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT false,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    ip_address VARCHAR(45),                     -- IPv4/IPv6
    user_agent VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Learning Points:**
- Token revocation pattern
- Security audit trail (IP, user agent)
- TTL management

#### 7. **audit_logs**
Security event tracking for compliance

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,              -- LOGIN, LOGOUT, ROLE_CHANGE, etc.
    entity_type VARCHAR(100),                  -- User, Order, Payment, etc.
    entity_id BIGINT,
    old_values JSONB,                          -- Before state
    new_values JSONB,                          -- After state
    status VARCHAR(50),                        -- SUCCESS, FAILURE
    error_message VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**Learning Points:**
- JSONB for flexible data structures
- Before/after tracking for audits
- Error logging for troubleshooting

#### 8. **password_reset_tokens**
Temporary tokens for password reset flow

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,              -- Time limit for reset
    is_used BOOLEAN DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Order Service Database (`order_db`)

### Entities & Relationships

```
Order (1) ──────────── (M) Order Items
 │
 ├─────────────────── (M) Order Status History
 │
├─────────────────── (M) Order Events (Event Sourcing)
│
└─────────────────── (M) Idempotency Keys
```

### Key Tables

#### 1. **orders**
Main order entity

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,   -- Human-readable: ORD-123456
    user_id BIGINT NOT NULL,                    -- Foreign key (User Service)
    status order_status DEFAULT 'PENDING',      -- Enum: PENDING, CONFIRMED, SHIPPED, etc.
    total_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_status payment_status DEFAULT 'PENDING',
    shipping_address JSONB NOT NULL,            -- {street, city, state, zipCode, country}
    billing_address JSONB,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    version BIGINT DEFAULT 0                    -- Optimistic locking
);
```

**Learning Points:**
- JSONB for flexible address storage
- Enum types for status
- Timestamp tracking for workflow
- Optimistic locking

#### 2. **order_items**
Line items within an order (1:M with orders)

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,                 -- Foreign key (Inventory Service)
    product_name VARCHAR(255) NOT NULL,        -- Denormalized for reporting
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price GENERATED ALWAYS AS (quantity * unit_price) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

**Learning Points:**
- Denormalization (product_name) for reporting
- Generated columns for calculations
- CHECK constraint for data validation
- CASCADE delete

#### 3. **order_events** (Event Sourcing)
Immutable event log for order lifecycle

```sql
CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50) UNIQUE NOT NULL,      -- UUID from Kafka
    order_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,          -- OrderCreated, OrderCancelled, etc.
    event_payload JSONB NOT NULL,              -- Full event details
    published_to_kafka BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

**Learning Points:**
- Immutable event log (append-only)
- Event sourcing pattern
- Kafka integration tracking

#### 4. **idempotency_keys**
Prevents duplicate order creation (idempotent API)

```sql
CREATE TABLE idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,               -- GET, POST, etc.
    request_body JSONB,
    response_body JSONB,
    response_status INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP                       -- Auto-cleanup after 24h
);
```

**Learning Points:**
- Idempotency pattern (safe retries)
- Request/response caching
- Client-driven deduplication

---

## Inventory Service Database (`inventory_db`)

### Key Tables

#### 1. **products**
Product catalog

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) UNIQUE NOT NULL,          -- Stock Keeping Unit
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    cost DECIMAL(10, 2),
    status product_status DEFAULT 'ACTIVE',    -- Enum: ACTIVE, INACTIVE, etc.
    supplier_id BIGINT,
    reorder_level INTEGER DEFAULT 10,          -- When to reorder
    reorder_quantity INTEGER DEFAULT 50,       -- How many to order
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);
```

**Learning Points:**
- SKU for external system integration
- Price management (selling + cost)
- Inventory management policy

#### 2. **stock_levels**
Current inventory count (1:1 with products)

```sql
CREATE TABLE stock_levels (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT UNIQUE NOT NULL,
    quantity_available INTEGER NOT NULL DEFAULT 0,  -- Ready to sell
    quantity_reserved INTEGER NOT NULL DEFAULT 0,   -- Pending orders
    quantity_damaged INTEGER NOT NULL DEFAULT 0,
    total_quantity GENERATED ALWAYS AS (
        quantity_available + quantity_reserved + quantity_damaged
    ) STORED,
    warehouse_location VARCHAR(100),           -- A-1-5 (Aisle-Shelf-Bin)
    last_counted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0                   -- Optimistic locking for updates
);
```

**Learning Points:**
- Stock state tracking (available, reserved, damaged)
- Generated columns
- Optimistic locking for concurrent updates
- Distributed locks (Redis) for critical sections

#### 3. **inventory_transactions**
Audit trail (immutable log)

```sql
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    transaction_type VARCHAR(100) NOT NULL,   -- PURCHASE, SALE, RETURN, DAMAGE
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(100),                -- order_id, purchase_order_id
    reference_type VARCHAR(50),
    reason VARCHAR(500),
    created_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);
```

**Learning Points:**
- Audit trail (append-only log)
- Reference tracking for traceability
- Created_by for accountability

#### 4. **distributed_locks**
Redis-backed locks for distributed synchronization (audit table)

```sql
CREATE TABLE distributed_locks (
    id BIGSERIAL PRIMARY KEY,
    resource_name VARCHAR(255) NOT NULL,       -- stock:product:123
    locked_by VARCHAR(100) NOT NULL,          -- Service name
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    version VARCHAR(50)                        -- Lock UUID
);
```

**Learning Points:**
- Distributed lock pattern
- Redis integration
- Lock expiration (deadlock prevention)

#### 5. **low_stock_alerts**
Reorder alerts for inventory management

```sql
CREATE TABLE low_stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    current_quantity INTEGER NOT NULL,
    reorder_level INTEGER NOT NULL,
    alert_sent_at TIMESTAMP,
    purchase_order_created BOOLEAN DEFAULT false,
    purchase_order_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

---

## Payment Service Database (`payment_db`)

### Key Tables

#### 1. **payments**
Payment records (PCI compliant)

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(50) UNIQUE NOT NULL,    -- External gateway ID
    order_id BIGINT NOT NULL,                  -- Foreign key (Order Service)
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'USD',
    status payment_status DEFAULT 'PENDING',   -- PENDING, CAPTURED, REFUNDED
    payment_method payment_method_type NOT NULL,
    payment_method_id BIGINT,                  -- Foreign key to payment_methods
    merchant_reference VARCHAR(100),           -- For reconciliation
    gateway_response JSONB,                    -- ENCRYPTED (sensitive!)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);
```

**Learning Points:**
- Never store raw credit card data (always tokenized)
- Encrypt sensitive gateway responses
- Separate payment methods for reusability
- Status tracking for payment lifecycle

#### 2. **payment_methods**
Saved payment instruments (tokenized, encrypted)

```sql
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type payment_method_type NOT NULL,
    encrypted_data VARCHAR(500) NOT NULL,     -- Spring Security Crypto
    token VARCHAR(100),                        -- Non-sensitive token
    last_four VARCHAR(4),                     -- For UI display
    expiry_month INTEGER,
    expiry_year INTEGER,
    card_holder_name VARCHAR(255),
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);
```

**Learning Points:**
- Tokenization pattern (PCI-DSS)
- Encryption for stored data
- Last-four for PCI compliance

#### 3. **refunds**
Refund tracking

```sql
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    refund_id VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255),                      -- CUSTOMER_REQUEST, DUPLICATE, etc.
    status VARCHAR(50) DEFAULT 'PENDING',     -- State machine
    gateway_response JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);
```

#### 4. **fraud_checks**
Fraud detection results

```sql
CREATE TABLE fraud_checks (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    check_type VARCHAR(100),                  -- AVS, CVV, VELOCITY, AMOUNT
    check_result VARCHAR(50),                 -- PASS, FAIL, REVIEW
    check_details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);
```

**Learning Points:**
- Fraud prevention rules
- Multiple check layers
- Audit trail for disputes

---

## Notification Service Database (`notification_db`)

### Key Tables

#### 1. **notifications**
Notification records

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    type notification_type NOT NULL,          -- EMAIL, SMS, PUSH, IN_APP
    subject VARCHAR(255),
    body TEXT NOT NULL,
    template_name VARCHAR(100),               -- order_confirmation, etc.
    template_variables JSONB,                 -- Variables for templating
    status notification_status DEFAULT 'PENDING',
    recipient_address VARCHAR(255),           -- Email, phone, device token
    priority VARCHAR(20) DEFAULT 'NORMAL',    -- CRITICAL, HIGH, NORMAL, LOW
    scheduled_for TIMESTAMP,                  -- Delayed sending
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Learning Points:**
- Templating pattern (template_name + template_variables)
- Retry mechanism
- Priority-based processing
- Delivery tracking

#### 2. **notification_templates**
Reusable notification templates

```sql
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type notification_type NOT NULL,
    subject_template VARCHAR(255),            -- For email
    body_template TEXT NOT NULL,
    description VARCHAR(500),
    language VARCHAR(10) DEFAULT 'en',
    variables JSONB,                          -- Expected variables
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. **notification_preferences**
User notification opt-ins

```sql
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    email_on_order_created BOOLEAN DEFAULT true,
    email_on_order_shipped BOOLEAN DEFAULT true,
    sms_on_payment_failed BOOLEAN DEFAULT true,
    push_on_order_update BOOLEAN DEFAULT true,
    unsubscribed_from_marketing BOOLEAN DEFAULT false,
    unsubscribed_from_all BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Learning Points:**
- User preferences pattern
- GDPR compliance (unsubscribe)
- Flexible opt-in/opt-out

#### 4. **failed_notifications** (Dead Letter Queue)
```sql
CREATE TABLE failed_notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    error_stacktrace TEXT,
    retry_count INTEGER DEFAULT 0,
    next_retry_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);
```

---

## Query Examples

### Get User with Roles and Permissions

```sql
SELECT 
    u.id, u.email, u.first_name,
    json_agg(DISTINCT r.name) as roles,
    json_agg(DISTINCT p.name) as permissions
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE u.email = 'user@example.com'
GROUP BY u.id, u.email, u.first_name;
```

### Get Order with Items

```sql
SELECT 
    o.id, o.order_number, o.total_amount,
    json_agg(
        json_build_object(
            'product_name', oi.product_name,
            'quantity', oi.quantity,
            'unit_price', oi.unit_price,
            'total_price', oi.total_price
        )
    ) as items
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.id = 1
GROUP BY o.id, o.order_number, o.total_amount;
```

### Check Stock Availability

```sql
SELECT 
    p.sku,
    p.name,
    sl.quantity_available,
    sl.quantity_reserved,
    CASE 
        WHEN sl.quantity_available <= p.reorder_level THEN 'LOW_STOCK'
        WHEN sl.quantity_available = 0 THEN 'OUT_OF_STOCK'
        ELSE 'IN_STOCK'
    END as status
FROM products p
JOIN stock_levels sl ON p.id = sl.product_id
WHERE sl.quantity_available < p.reorder_level
ORDER BY sl.quantity_available ASC;
```

### Get Payment History

```sql
SELECT 
    p.payment_id,
    p.amount,
    p.currency,
    p.status,
    json_agg(
        json_build_object(
            'type', pt.transaction_type,
            'amount', pt.amount,
            'status', pt.status,
            'created_at', pt.created_at
        ) ORDER BY pt.created_at
    ) as transactions
FROM payments p
LEFT JOIN payment_transactions pt ON p.id = pt.payment_id
WHERE p.user_id = $1
GROUP BY p.id, p.payment_id, p.amount, p.currency, p.status
ORDER BY p.created_at DESC;
```

---

## Migration Strategy

### Initial Setup (V1.0)
All tables and initial data are created via Flyway migrations:
```
V1.0__User_initial_schema.sql
V1.0__Order_initial_schema.sql
V1.0__Inventory_initial_schema.sql
V1.0__Payment_initial_schema.sql
V1.0__Notification_initial_schema.sql
```

### Adding New Tables (V2.0+)
Create new migration files:
```sql
-- V2.1__Add_subscription_table.sql
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ...
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Flyway automatically applies migrations in version order.

---

## Best Practices

### 1. Naming Conventions
- Table names: snake_case, plural (e.g. `order_items`)
- Columns: snake_case (e.g. `created_at`, `is_active`)
- Primary keys: `id BIGSERIAL`
- Foreign keys: `[table_name]_id` (e.g. `user_id`, `order_id`)

### 2. Timestamps
- Always include: `created_at`, `updated_at`
- Use: `DEFAULT CURRENT_TIMESTAMP`
- Update `updated_at` with trigger or application code

### 3. Concurrency Control
- Use `version BIGINT` for optimistic locking
- Use Redis for distributed locks
- Never use pessimistic locks (row-level locks)

### 4. Data Types
- IDs: `BIGSERIAL PRIMARY KEY`
- Money: `DECIMAL(10, 2)` (never float!)
- Strings: `VARCHAR(length)`
- JSON: `JSONB` (not JSON)
- Booleans: `BOOLEAN DEFAULT false`
- Dates: `TIMESTAMP` (not DATE)

### 5. Constraints
- Always use CHECK constraints: `CHECK (amount > 0)`
- Foreign keys: `ON DELETE CASCADE` or `ON DELETE SET NULL`
- Unique: For natural keys (email, sku, order_number)

### 6. Indexes
- Index foreign keys
- Index frequently searched columns
- Index columns in WHERE clauses
- Index TIMESTAMP columns for range queries
- Avoid over-indexing (slows writes)

---

## Security Considerations

### PCI-DSS Compliance (Payment Service)
- ✅ Never store full credit card numbers
- ✅ Use tokenization from payment gateway
- ✅ Encrypt sensitive data with Spring Security Crypto
- ✅ Log all access to payment data
- ✅ Separate payment_methods from payment transactions

### GDPR Compliance (User Service)
- ✅ Soft deletion (deleted_at field)
- ✅ Audit logs for data access
- ✅ Encrypt sensitive personal data
- ✅ TTU policy for tokens and reset links

### General Security
- ✅ Always use parameterized queries (prevent SQL injection)
- ✅ Never store plaintext passwords (use BCrypt hashing)
- ✅ Audit all modifications to sensitive tables
- ✅ Encrypt data in transit (HTTPS everywhere)

---

## Links

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Flyway Documentation](https://flywaydb.org/documentation)
- [JPA Best Practices](https://spring.io/guides/gs/accessing-data-jpa)
- [Hibernate Docs](https://hibernate.org/orm/documentation/)
