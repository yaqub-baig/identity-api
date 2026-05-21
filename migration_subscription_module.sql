-- Subscription Module Migration for Identity-API
-- This migration adds subscription management tables to the identity database

-- Subscription Plans Table
CREATE TABLE IF NOT EXISTS subscription_plan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    duration_months INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    applicable_to VARCHAR(50) DEFAULT 'ALL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Platform Subscriptions Table
CREATE TABLE IF NOT EXISTS platform_subscription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    entity_id INT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    plan_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) DEFAULT 'NOT_INITIATED',
    gateway_transaction_id VARCHAR(100),
    gateway_order_reference VARCHAR(100),
    payment_amount DECIMAL(10,2),
    payment_reference VARCHAR(100),
    payment_method VARCHAR(50),
    payment_initiated_at TIMESTAMP NULL,
    payment_completed_at TIMESTAMP NULL,
    activated_by INT,
    activated_at TIMESTAMP NULL,
    is_trial BOOLEAN DEFAULT FALSE,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),

    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status),
    INDEX idx_gateway_txn (gateway_transaction_id)
);

-- Seed default subscription plans
INSERT INTO subscription_plan (name, duration_months, price, description, applicable_to) VALUES
('Monthly Plan', 1, 99.00, 'Monthly subscription for dashboard access', 'ALL'),
('Semi-Annual Plan', 6, 499.00, '6-month subscription (16% savings)', 'ALL'),
('Annual Plan', 12, 899.00, '12-month subscription (25% savings)', 'ALL');
