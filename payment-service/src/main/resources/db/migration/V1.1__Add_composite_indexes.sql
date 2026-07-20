-- V1.1: Add composite indexes for common multi-column query patterns

-- =====================================================
-- payments table
-- =====================================================

-- Account page: "show user X's payments filtered by status, newest first"
-- Replaces bitmap-AND of idx_payments_user_id + idx_payments_status
CREATE INDEX idx_payments_user_status_created
    ON payments (user_id, status, created_at DESC);

-- =====================================================
-- payment_methods table
-- =====================================================

-- Checkout page: "get active payment methods for user, default first"
-- Partial index: only active methods (small fraction of all rows)
CREATE INDEX idx_payment_methods_user_active
    ON payment_methods (user_id, is_default DESC)
    WHERE is_active = true;

-- =====================================================
-- webhook_events table
-- =====================================================

-- Webhook retry poller: "unprocessed events ordered by age"
-- Partial index: only unprocessed rows (transient, always tiny)
CREATE INDEX idx_webhook_events_unprocessed
    ON webhook_events (created_at ASC)
    WHERE is_processed = false;

-- =====================================================
-- refunds table
-- =====================================================

-- Refund dashboard: "pending refunds for a payment, by creation date"
CREATE INDEX idx_refunds_payment_status_created
    ON refunds (payment_id, status, created_at DESC);
