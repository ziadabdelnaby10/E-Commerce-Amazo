-- Inventory Service Database Schema
-- Handles: Products, stock levels, inventory tracking, distributed locks

-- =====================================================
-- Product Status Enum
-- =====================================================
DO $$ BEGIN
    CREATE TYPE product_status AS ENUM (
        'ACTIVE',
        'INACTIVE',
        'DISCONTINUED',
        'OUT_OF_STOCK'
    );
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Products Table (Core Entity)
-- =====================================================
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,  -- Stock Keeping Unit
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    cost DECIMAL(10, 2),  -- Cost price
    status product_status DEFAULT 'ACTIVE',
    supplier_id BIGINT,  -- Future: link to Supplier Service
    reorder_level INTEGER DEFAULT 10,  -- When to reorder
    reorder_quantity INTEGER DEFAULT 50,  -- How many to reorder
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0  -- Optimistic locking
);

-- Indexes
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_name ON products USING GIN(to_tsvector('english', name));  -- Full-text search

-- =====================================================
-- Stock Levels Table (Inventory Tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS stock_levels (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,  -- Pending orders
    quantity_damaged INTEGER NOT NULL DEFAULT 0,
    total_quantity GENERATED ALWAYS AS (quantity_available + quantity_reserved + quantity_damaged) STORED,
    warehouse_location VARCHAR(100),  -- e.g., 'A-1-5'
    last_counted_at TIMESTAMP,  -- Physical inventory date
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,  -- Optimistic locking
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Indexes
CREATE UNIQUE INDEX idx_stock_levels_product_id ON stock_levels(product_id);
CREATE INDEX idx_stock_levels_quantity_available ON stock_levels(quantity_available);

-- =====================================================
-- Inventory Transactions (Audit Trail)
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    transaction_type VARCHAR(100) NOT NULL,  -- 'PURCHASE', 'SALE', 'ADJUSTMENT', 'DAMAGE', 'RETURN'
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(100),  -- e.g., order_id or purchase_order_id
    reference_type VARCHAR(50),  -- 'ORDER', 'PURCHASE_ORDER', 'ADJUSTMENT'
    reason VARCHAR(500),
    created_by VARCHAR(100),  -- User or system
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_inventory_transactions_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inventory_transactions_type ON inventory_transactions(transaction_type);
CREATE INDEX idx_inventory_transactions_reference_id ON inventory_transactions(reference_id);
CREATE INDEX idx_inventory_transactions_created_at ON inventory_transactions(created_at DESC);

-- =====================================================
-- Inventory Events (Event Sourcing)
-- =====================================================
CREATE TABLE IF NOT EXISTS inventory_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL UNIQUE,  -- UUID from Kafka event
    product_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,  -- 'InventoryReserved', 'InventoryReleased', 'StockLevelUpdated'
    event_payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX idx_inventory_events_product_id ON inventory_events(product_id);
CREATE INDEX idx_inventory_events_event_type ON inventory_events(event_type);
CREATE INDEX idx_inventory_events_created_at ON inventory_events(created_at DESC);

-- =====================================================
-- Distributed Locks (Redis stores actual locks, this is for audit)
-- =====================================================
CREATE TABLE IF NOT EXISTS distributed_locks (
    id BIGSERIAL PRIMARY KEY,
    resource_name VARCHAR(255) NOT NULL,  -- e.g., 'stock:product:123'
    locked_by VARCHAR(100) NOT NULL,  -- Service/thread that acquired lock
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    version VARCHAR(50)  -- UUID for the lock
);

CREATE INDEX idx_distributed_locks_resource ON distributed_locks(resource_name);
CREATE INDEX idx_distributed_locks_expires_at ON distributed_locks(expires_at);

-- =====================================================
-- Low Stock Alerts
-- =====================================================
CREATE TABLE IF NOT EXISTS low_stock_alerts (
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

CREATE INDEX idx_low_stock_alerts_product_id ON low_stock_alerts(product_id);
CREATE INDEX idx_low_stock_alerts_resolved_at ON low_stock_alerts(resolved_at);

-- =====================================================
-- Sample Products Data
-- =====================================================
INSERT INTO products (sku, name, description, category, price, cost, status, reorder_level, reorder_quantity) VALUES
    ('SKU-001', 'Laptop Pro', 'High-performance laptop', 'Electronics', 999.99, 600.00, 'ACTIVE', 5, 10),
    ('SKU-002', 'Wireless Mouse', 'Ergonomic wireless mouse', 'Accessories', 29.99, 10.00, 'ACTIVE', 20, 50),
    ('SKU-003', 'USB-C Cable', 'High-speed USB-C cable', 'Accessories', 9.99, 2.00, 'ACTIVE', 50, 200),
    ('SKU-004', 'Monitor 27"', '4K Monitor', 'Electronics', 399.99, 250.00, 'ACTIVE', 3, 5),
    ('SKU-005', 'Mechanical Keyboard', 'RGB Mechanical Keyboard', 'Accessories', 149.99, 75.00, 'ACTIVE', 10, 25)
ON CONFLICT (sku) DO NOTHING;

-- =====================================================
-- Initialize Stock Levels for Sample Products
-- =====================================================
INSERT INTO stock_levels (product_id, quantity_available, warehouse_location)
SELECT id, 100, 'A-1-' || CAST((ROW_NUMBER() OVER (ORDER BY id)) AS TEXT) FROM products
ON CONFLICT (product_id) DO NOTHING;
