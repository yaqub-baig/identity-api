-- Subscription Simplification Migration
-- Transforms from per-entity to per-user subscription model with 3 fixed plans and 1-month free trial

-- =====================================================
-- Phase 1: Make entity columns nullable (for user-based subscriptions)
-- =====================================================

ALTER TABLE platform_subscription
    MODIFY COLUMN entity_id INT NULL,
    MODIFY COLUMN entity_type VARCHAR(50) NULL;

-- =====================================================
-- Phase 2: Make applicableTo nullable (no longer used for filtering)
-- =====================================================

ALTER TABLE subscription_plan
    MODIFY COLUMN applicable_to VARCHAR(50) NULL;

-- =====================================================
-- Phase 3: Deactivate existing plans
-- =====================================================

UPDATE subscription_plan SET is_active = FALSE;

-- =====================================================
-- Phase 4: Insert 3 standard plans (prices set via admin later)
-- =====================================================

INSERT INTO subscription_plan (name, duration_months, price, description, applicable_to, is_active) VALUES
('Monthly', 1, 0.00, 'Monthly subscription - unlocks all dashboards', NULL, TRUE),
('Semi-Annual', 6, 0.00, '6-month subscription - unlocks all dashboards', NULL, TRUE),
('Annual', 12, 0.00, '12-month subscription - unlocks all dashboards', NULL, TRUE);

-- =====================================================
-- Phase 5: Add index for user-based lookups
-- =====================================================

CREATE INDEX idx_user_status_enddate ON platform_subscription(user_id, status, end_date);
