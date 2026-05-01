-- Migration to create Audit Logs table
-- Version: V3

CREATE TABLE IF NOT EXISTS orders.audit_logs (
    id UUID PRIMARY KEY,
    action_name VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    role VARCHAR(50),
    target_id VARCHAR(100), -- ID của Order/Ticket/Item bị tác động
    details TEXT,           -- Dữ liệu chi tiết trước/sau thay đổi (JSON)
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_action_name ON orders.audit_logs(action_name);
CREATE INDEX idx_audit_logs_user_id ON orders.audit_logs(user_id);
CREATE INDEX idx_audit_logs_target_id ON orders.audit_logs(target_id);
