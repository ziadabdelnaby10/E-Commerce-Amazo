-- Payment Service Database Schema
-- Handles: Payment processing, transactions, sensitive payment data (encrypted)

-- =====================================================
-- Payment Status Enum
-- =====================================================
DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM (
        'PENDING',
        'AUTHORIZED',
        'CAPTURED',
        'DECLINED',
        'FAILED',
        'REFUNDED',
        'CANCELLED'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Payment Method Type Enum
-- =====================================================
DO $$ BEGIN
    CREATE TYPE payment_method_type AS ENUM (
        'CREDIT_CARD',
        'DEBIT_CARD',
        'PAYPAL',
        'WIRE_TRANSFER',
        'CRYPTOCURRENCY'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Payments Table (Core Entity)
-- =====================================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(50) NOT NULL UNIQUE,  -- External payment gateway ID
    order_id BIGINT NOT NULL,  -- Foreign key to Order Service
    user_id BIGINT NOT NULL,  -- Foreign key to User Service
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'USD',
    status payment_status DEFAULT 'PENDING',
    payment_method payment_method_type NOT NULL,
    payment_method_id BIGINT,  -- Foreign key to PaymentMethods table
    merchant_reference VARCHAR(100),  -- Reference for merchant reconciliation
    gateway_response JSONB,  -- Response from payment gateway (can contain sensitive data - ENCRYPTED)
    metadata JSONB,  -- Custom metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,
    version BIGINT DEFAULT 0  -- Optimistic locking
);

-- Indexes
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_payment_id ON payments(payment_id);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- =====================================================
-- Payment Methods (Saved Cards, etc.) - PCI COMPLIANT
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type payment_method_type NOT NULL,
    encrypted_data VARCHAR(500) NOT NULL,  -- Encrypted card/account data
    token VARCHAR(100),  -- Tokenized representation (non-sensitive)
    last_four VARCHAR(4),  -- Last 4 digits for display
    expiry_month INTEGER,
    expiry_year INTEGER,
    card_holder_name VARCHAR(255),
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_token ON payment_methods(token);
CREATE INDEX idx_payment_methods_is_default ON payment_methods(is_default);

-- =====================================================
-- Payment Transactions (Line items / breakdown)
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,  -- 'AUTHORIZATION', 'CAPTURE', 'REFUND', 'CHARGEBACK'
    amount DECIMAL(10, 2) NOT NULL,
    gateway_transaction_id VARCHAR(100),  -- External transaction ID
    status payment_status NOT NULL,
    response_code VARCHAR(10),
    response_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX idx_payment_transactions_type ON payment_transactions(transaction_type);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at DESC);

-- =====================================================
-- Refunds Table
-- =====================================================
CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    refund_id VARCHAR(50) NOT NULL UNIQUE,  -- External refund ID
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255),  -- 'CUSTOMER_REQUEST', 'DUPLICATE', 'FRAUD', etc.
    status VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, PROCESSING, COMPLETED, FAILED
    gateway_response JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_refund_id ON refunds(refund_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_created_at ON refunds(created_at DESC);

-- =====================================================
-- Payment Audit Log (Compliance & Security)
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_audit_log (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT,
    action VARCHAR(100) NOT NULL,  -- 'CREATED', 'AUTHORIZED', 'CAPTURED', 'REFUNDED', 'FAILED'
    actor VARCHAR(100),  -- 'SYSTEM', 'USER', or service name
    old_status payment_status,
    new_status payment_status,
    reason VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_payment_audit_log_payment_id ON payment_audit_log(payment_id);
CREATE INDEX idx_payment_audit_log_action ON payment_audit_log(action);
CREATE INDEX idx_payment_audit_log_created_at ON payment_audit_log(created_at DESC);

-- =====================================================
-- Fraud Detection (Simple rules-based system)
-- =====================================================
CREATE TABLE IF NOT EXISTS fraud_checks (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    check_type VARCHAR(100),  -- 'AVS', 'CVV', 'VELOCITY', 'AMOUNT_THRESHOLD'
    check_result VARCHAR(50),  -- 'PASS', 'FAIL', 'REVIEW'
    check_details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_fraud_checks_payment_id ON fraud_checks(payment_id);
CREATE INDEX idx_fraud_checks_check_result ON fraud_checks(check_result);

-- =====================================================
-- Webhook Events (For monitoring payment gateway callbacks)
-- =====================================================
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    webhook_id VARCHAR(50) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,  -- 'payment.completed', 'payment.failed', etc.
    payment_id BIGINT,
    payload JSONB NOT NULL,
    is_processed BOOLEAN DEFAULT false,
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMP,
    processing_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_webhook_events_payment_id ON webhook_events(payment_id);
CREATE INDEX idx_webhook_events_event_type ON webhook_events(event_type);
CREATE INDEX idx_webhook_events_is_processed ON webhook_events(is_processed);
CREATE INDEX idx_webhook_events_created_at ON webhook_events(created_at DESC);

-- =====================================================
-- PCI Compliance Notes
-- =====================================================
-- 1. Sensitive data (card numbers, CVV) should NEVER be stored unencrypted
-- 2. Use tokenization from payment gateway when possible
-- 3. Encrypt sensitive data in transit (HTTPS everywhere)
-- 4. Log all access to payment data
-- 5. Regular security audits and penetration testing
-- 6. Use Spring Security Crypto for encryption/decryption
-- 7. Never log full card numbers or CVV
