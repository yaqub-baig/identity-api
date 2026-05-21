-- ============================================================================
-- Database Migration: Update Push Subscription for Multi-Role Support
-- ============================================================================
-- This migration updates the push_subscription table to support multi-role
-- subscriptions by changing the unique constraint from (user_id, fcm_token)
-- to (user_id, fcm_token(191), role).
--
-- Note: fcm_token(191) uses a prefix index to stay within MySQL's 3072 byte
-- index limit while still preventing duplicates effectively.
--
-- WARNING: Run this script during low traffic period or maintenance window
-- ============================================================================

USE autoadda_identity; -- Replace with your actual database name

-- Step 1: Show current constraints (for reference)
SHOW INDEXES FROM push_subscription;

-- Step 2: Drop the old unique constraint
-- The old constraint prevents same user+token from having multiple roles
ALTER TABLE push_subscription
DROP INDEX UK15plavur2eqek11fu97gtcahn;

-- Step 3: Add the new unique constraint with prefix index
-- This allows same user+token to subscribe for different roles
-- Using fcm_token(191) to stay within index size limits
-- 191 chars * 4 bytes (utf8mb4) = 764 bytes per token in index
ALTER TABLE push_subscription
ADD CONSTRAINT unique_user_token_role
UNIQUE (user_id, fcm_token(191), role);

-- Step 4: Verify the new constraint exists
-- Expected output: Shows unique_user_token_role constraint
SHOW INDEXES FROM push_subscription WHERE Key_name = 'unique_user_token_role';

-- ============================================================================
-- Post-Migration Verification:
-- ============================================================================
-- Run this query to see current subscriptions:
-- SELECT id, user_id, role, LEFT(fcm_token, 50) as token_preview, device_info, subscribed_at
-- FROM push_subscription
-- ORDER BY user_id, fcm_token, role;
--
-- ============================================================================
-- Post-Migration Notes:
-- ============================================================================
-- After running this migration, users can have multiple subscriptions:
-- - (userId=1, token=abc..., role=CUSTOMER)
-- - (userId=1, token=abc..., role=SERVICE_PROVIDER)
-- - (userId=1, token=abc..., role=ROLE_GARAGE_TECH)
--
-- Each role gets a separate subscription entry for the same device/token.
-- ============================================================================
