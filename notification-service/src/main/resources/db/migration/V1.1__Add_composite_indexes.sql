-- V1.1: Add composite indexes for common multi-column query patterns

-- =====================================================
-- notifications table
-- =====================================================

-- User inbox: "show user X's notifications by type, newest first"
-- Replaces bitmap-AND of idx_notifications_user_id + idx_notifications_type
CREATE INDEX idx_notifications_user_type_created
    ON notifications (user_id, type, created_at DESC);

-- Retry poller: "PENDING notifications ready to retry right now, by priority"
-- Partial index: only PENDING rows (always a small fraction)
-- ORDER matches the poller's SELECT ... ORDER BY priority DESC, next_retry_at ASC
CREATE INDEX idx_notifications_retry_poller
    ON notifications (priority DESC, next_retry_at ASC)
    WHERE status = 'PENDING';

-- =====================================================
-- notification_events table (Kafka consumer deduplication)
-- =====================================================

-- Dedup check + unprocessed poller: "unprocessed events by age"
-- Partial index: only unprocessed events (transient)
CREATE INDEX idx_notification_events_unprocessed
    ON notification_events (created_at ASC)
    WHERE processed = false;

-- =====================================================
-- failed_notifications table
-- =====================================================

-- Dead-letter retry scheduler: "failures ready for next retry attempt"
CREATE INDEX idx_failed_notifications_ready
    ON failed_notifications (next_retry_time ASC)
    WHERE next_retry_time IS NOT NULL;
