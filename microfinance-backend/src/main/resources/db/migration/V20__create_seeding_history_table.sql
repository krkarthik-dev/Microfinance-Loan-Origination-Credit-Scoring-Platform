-- V20__create_seeding_history_table.sql
-- Creates persistent tracking table for US65 one-time dev/test baseline seeding idempotency

CREATE TABLE IF NOT EXISTS seeding_history (
    id SERIAL PRIMARY KEY,
    script_name VARCHAR(100) NOT NULL UNIQUE,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_seeding_history_script_name ON seeding_history(script_name);
