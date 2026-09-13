-- V1.2: Persist the customer's email address on the order.
--
-- Rationale: notification-service consumes order/payment events from Kafka, where there is no
-- authenticated user context and therefore no token to call customer-service with. Carrying the
-- recipient address inside the event payload removes that cross-service call entirely and makes
-- the event self-contained (an event must not depend on a live lookup to be interpretable).

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);

COMMENT ON COLUMN orders.customer_email IS
    'Snapshot of the customer email at order time, propagated into order events for notifications.';

