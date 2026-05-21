-- Adds columns required by the admin user-dashboard endpoints.
-- MySQL 8.x. Idempotent: safe to run multiple times.

-- ---------- users.status ----------
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND COLUMN_NAME  = 'status'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN status VARCHAR(30)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- users.created_at ----------
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND COLUMN_NAME  = 'created_at'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN created_at TIMESTAMP NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill existing rows so the dashboard cards make sense.
UPDATE users SET status     = 'ACTIVE'           WHERE status     IS NULL;
UPDATE users SET created_at = CURRENT_TIMESTAMP  WHERE created_at IS NULL;

-- ---------- idx_users_status ----------
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND INDEX_NAME   = 'idx_users_status'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_users_status ON users (status)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- idx_users_created_at ----------
SET @idx_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND INDEX_NAME   = 'idx_users_created_at'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_users_created_at ON users (created_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
