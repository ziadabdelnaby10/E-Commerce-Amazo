-- Notification Service Database Schema
-- Handles: Email, SMS, push notifications, notification logs, preferences

-- =====================================================
-- Notification Type Enum
-- =====================================================
DO $$ BEGIN
    CREATE TYPE notification_type AS ENUM (
        'EMAIL',
        'SMS',
        'PUSH',
        'IN_APP'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Notification Status Enum
-- =====================================================
DO $$ BEGIN
    CREATE TYPE notification_status AS ENUM (
        'PENDING',
        'SENT',
        'DELIVERED',
        'FAILED',
        'BOUNCED',
        'UNSUBSCRIBED'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Notifications Table
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(50) NOT NULL UNIQUE,  -- UUID
    user_id BIGINT NOT NULL,  -- Foreign key to User Service
    type notification_type NOT NULL,
    subject VARCHAR(255),  -- For email
    body TEXT NOT NULL,
    template_name VARCHAR(100),  -- e.g., 'order_confirmation', 'payment_success'
    template_variables JSONB,  -- Variables for template rendering
    status notification_status DEFAULT 'PENDING',
    recipient_address VARCHAR(255),  -- Email, phone, device token
    priority VARCHAR(20) DEFAULT 'NORMAL',  -- CRITICAL, HIGH, NORMAL, LOW
    scheduled_for TIMESTAMP,  -- For delayed sending
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP,
    metadata JSONB,  -- Custom metadata, event references
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_scheduled_for ON notifications(scheduled_for);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_notification_id ON notifications(notification_id);

-- =====================================================
-- User Notification Preferences
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,  -- Foreign key to User Service
    email_on_order_created BOOLEAN DEFAULT true,
    email_on_order_shipped BOOLEAN DEFAULT true,
    email_on_order_delivered BOOLEAN DEFAULT true,
    email_on_payment_success BOOLEAN DEFAULT true,
    email_on_payment_failed BOOLEAN DEFAULT true,
    email_on_inventory_alert BOOLEAN DEFAULT false,
    sms_on_order_shipped BOOLEAN DEFAULT false,
    sms_on_payment_failed BOOLEAN DEFAULT true,
    push_on_order_update BOOLEAN DEFAULT true,
    push_on_payment_update BOOLEAN DEFAULT true,
    unsubscribed_from_marketing BOOLEAN DEFAULT false,
    unsubscribed_from_all BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- =====================================================
-- Notification Templates
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type notification_type NOT NULL,
    subject_template VARCHAR(255),  -- For email templates
    body_template TEXT NOT NULL,  -- HTML for email, plain text for SMS
    description VARCHAR(500),
    language VARCHAR(10) DEFAULT 'en',
    variables JSONB,  -- List of expected variables
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_templates_name_type UNIQUE (name, type)
);

-- Indexes
CREATE INDEX idx_notification_templates_name ON notification_templates(name);
CREATE INDEX idx_notification_templates_type ON notification_templates(type);
CREATE INDEX idx_notification_templates_is_active ON notification_templates(is_active);

-- =====================================================
-- Insert Default Templates
-- =====================================================
INSERT INTO notification_templates (name, type, subject_template, body_template, description) VALUES
    ('order_confirmation', 'EMAIL',
     'Order Confirmation - {{orderNumber}}',
     '<h1>Thank you for your order!</h1><p>Order #{{orderNumber}} has been created successfully.</p>',
     'Sent when order is created'),
    ('order_shipped', 'EMAIL',
     'Your Order Has Been Shipped - {{orderNumber}}',
     '<p>Your order {{orderNumber}} has been shipped. Tracking: {{trackingNumber}}</p>',
     'Sent when order is shipped'),
    ('payment_success', 'EMAIL',
     'Payment Confirmed - {{orderNumber}}',
     '<p>Payment of {{amount}} {{currency}} has been successfully processed.</p>',
     'Sent when payment is successful'),
    ('payment_failed', 'EMAIL',
     'Payment Failed - {{orderNumber}}',
     '<p>Payment for order {{orderNumber}} failed. Reason: {{failureReason}}</p>',
     'Sent when payment fails'),
    ('order_confirmation', 'SMS',
     NULL,
     'Order {{orderNumber}} confirmed. Total: {{amount}}. Track at: {{trackingUrl}}',
     'SMS confirmation for order'),
    ('payment_success', 'SMS',
     NULL,
     'Payment of {{amount}} {{currency}} confirmed for order {{orderNumber}}.',
     'SMS confirmation for payment')
ON CONFLICT (name, type) DO NOTHING;

-- =====================================================
-- Notification Events (From Kafka)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL UNIQUE,  -- UUID from Kafka
    event_type VARCHAR(100) NOT NULL,  -- 'OrderCreated', 'PaymentCompleted', etc.
    aggregate_id BIGINT NOT NULL,  -- order_id, payment_id, etc.
    aggregate_type VARCHAR(50) NOT NULL,  -- 'Order', 'Payment', etc.
    event_payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT false,
    processed_at TIMESTAMP,
    processing_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_notification_events_event_id ON notification_events(event_id);
CREATE INDEX idx_notification_events_event_type ON notification_events(event_type);
CREATE INDEX idx_notification_events_aggregate_id ON notification_events(aggregate_id);
CREATE INDEX idx_notification_events_processed ON notification_events(processed);
CREATE INDEX idx_notification_events_created_at ON notification_events(created_at DESC);

-- =====================================================
-- Notification Log (History)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_log (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,  -- 'CREATED', 'SENT', 'FAILED', 'RETRIED', 'BOUNCED'
    details JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_notification_log_notification_id ON notification_log(notification_id);
CREATE INDEX idx_notification_log_action ON notification_log(action);
CREATE INDEX idx_notification_log_timestamp ON notification_log(timestamp DESC);

-- =====================================================
-- Failed Notifications (Dead Letter Queue)
-- =====================================================
CREATE TABLE IF NOT EXISTS failed_notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    error_stacktrace TEXT,
    retry_count INTEGER DEFAULT 0,
    next_retry_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_failed_notifications_notification_id ON failed_notifications(notification_id);
CREATE INDEX idx_failed_notifications_retry_count ON failed_notifications(retry_count);
CREATE INDEX idx_failed_notifications_next_retry_time ON failed_notifications(next_retry_time);

-- =====================================================
-- Email Bounces (For compliance & list cleaning)
-- =====================================================
CREATE TABLE IF NOT EXISTS email_bounces (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    bounce_type VARCHAR(50),  -- 'HARD', 'SOFT', 'COMPLAINT'
    bounce_reason VARCHAR(500),
    bounced_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_email_bounces_user_id ON email_bounces(user_id);
CREATE INDEX idx_email_bounces_email_address ON email_bounces(email_address);
CREATE INDEX idx_email_bounces_bounce_type ON email_bounces(bounce_type);

-- =====================================================
-- SMS Delivery Reports
-- =====================================================
CREATE TABLE IF NOT EXISTS sms_delivery_reports (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    gateway_message_id VARCHAR(100),
    delivery_status VARCHAR(50),  -- 'DELIVERED', 'FAILED', 'PENDING'
    status_code VARCHAR(10),
    status_message TEXT,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_sms_delivery_reports_notification_id ON sms_delivery_reports(notification_id);
CREATE INDEX idx_sms_delivery_reports_delivery_status ON sms_delivery_reports(delivery_status);
