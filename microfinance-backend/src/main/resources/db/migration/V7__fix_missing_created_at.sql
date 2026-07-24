-- V7__fix_missing_created_at.sql

-- Add missing created_at columns that were introduced when extending BaseEntity
ALTER TABLE credit_scores
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE loan_decisions
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
