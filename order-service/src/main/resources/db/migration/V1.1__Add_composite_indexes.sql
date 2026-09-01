-- V1.1: Add composite indexes for common multi-column query patterns
-- These replace bitmap-AND operations on separate single-column indexes
-- with efficient single-index scans, and add partial indexes for hot
-- subset queries that benefit from staying in the buffer pool.

-- =====================================================
-- orders table
-- =====================================================

-- Most common dashboard query: "show user X's orders by status, newest first"
-- Replaces bitmap-AND of idx_orders_user_id + idx_orders_status + sort by created_at
CREATE INDEX IF NOT EXISTS idx_orders_user_status_created
    ON orders (user_id, status, created_at DESC);

-- Ops dashboard: "pending orders awaiting payment authorization"
-- Partial index keeps this tiny (only PENDING rows) → stays memory-resident
CREATE INDEX IF NOT EXISTS idx_orders_pending_payment
    ON orders (payment_status, created_at DESC)
    WHERE status = 'PENDING';

-- =====================================================
-- order_events table (outbox / Kafka poller)
-- =====================================================

-- Outbox poller: "find events not yet published to Kafka, by age"
-- Partial index only covers unpublished rows (tiny fraction of the table)
CREATE INDEX IF NOT EXISTS idx_order_events_unpublished
    ON order_events (created_at ASC)
    WHERE published_to_kafka = false;

-- =====================================================
-- idempotency_keys table
-- =====================================================

-- TTL-cleanup job: "delete expired keys for a given user"
CREATE INDEX IF NOT EXISTS idx_idempotency_user_expires
    ON idempotency_keys (user_id, expires_at)
    WHERE expires_at IS NOT NULL;
