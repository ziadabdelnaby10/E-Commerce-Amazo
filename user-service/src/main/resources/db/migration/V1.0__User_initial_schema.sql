-- User Service Database Schema
-- Handles: User accounts, authentication, roles, permissions

-- DROP SCHEMA IF EXISTS public CASCADE;
-- CREATE SCHEMA public;

-- =====================================================
-- Roles Table (Static Reference Data)
-- =====================================================
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for role lookups
CREATE INDEX idx_roles_name ON roles(name);

-- =====================================================
-- Users Table (Core Entity)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0 -- Optimistic locking
);

-- Indexes for common queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- =====================================================
-- User Roles Mapping (Many-to-Many)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,  -- User who assigned the role
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- Permissions Reference Table
-- =====================================================
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    category VARCHAR(50),  -- e.g., 'orders', 'payments', 'admin'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permissions_category ON permissions(category);

-- =====================================================
-- Role Permissions Mapping (Many-to-Many)
-- =====================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- =====================================================
-- JWT Refresh Tokens (Cache in Redis, but track in DB)
-- =====================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT false,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    ip_address VARCHAR(45),  -- IPv4 or IPv6
    user_agent VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_is_revoked ON refresh_tokens(is_revoked);

-- =====================================================
-- Audit Log (Track user actions for security)
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,  -- e.g., 'LOGIN', 'LOGOUT', 'PASSWORD_CHANGED'
    entity_type VARCHAR(100),       -- e.g., 'User', 'Order'
    entity_id BIGINT,
    old_values JSONB,               -- Previous values (for updates)
    new_values JSONB,               -- New values (for updates)
    status VARCHAR(50),             -- SUCCESS, FAILURE
    error_message VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for audit queries
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =====================================================
-- Password Reset Tokens (Temporary, short-lived)
-- =====================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

-- =====================================================
-- Insert Default Roles
-- =====================================================
INSERT INTO roles (name, description) VALUES 
    ('ROLE_ADMIN', 'Administrator - full system access'),
    ('ROLE_USER', 'Regular user - limited access'),
    ('ROLE_SUPPORT', 'Support staff - customer service access'),
    ('ROLE_SYSTEM', 'Internal service account')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- Insert Default Permissions
-- =====================================================
INSERT INTO permissions (name, category, description) VALUES 
    ('VIEW_ORDERS', 'orders', 'Can view orders'),
    ('CREATE_ORDER', 'orders', 'Can create orders'),
    ('CANCEL_ORDER', 'orders', 'Can cancel orders'),
    ('VIEW_PAYMENTS', 'payments', 'Can view payments'),
    ('PROCESS_PAYMENT', 'payments', 'Can process payments'),
    ('REFUND_PAYMENT', 'payments', 'Can refund payments'),
    ('VIEW_USERS', 'admin', 'Can view all users'),
    ('CREATE_USER', 'admin', 'Can create users'),
    ('MODIFY_USER', 'admin', 'Can modify users'),
    ('DELETE_USER', 'admin', 'Can delete users'),
    ('VIEW_INVENTORY', 'inventory', 'Can view inventory'),
    ('MODIFY_INVENTORY', 'inventory', 'Can modify inventory')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- Assign Permissions to Roles
-- =====================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.category IN ('orders')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_SUPPORT' AND p.category IN ('orders', 'payments')
ON CONFLICT DO NOTHING;
