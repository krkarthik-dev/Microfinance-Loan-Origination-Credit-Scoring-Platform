-- =============================================================================
-- V3__add_employment_and_income.sql
-- US09: Add Employment Type and Monthly Income to user_profiles
-- =============================================================================

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS employment_type VARCHAR(30),
ADD COLUMN IF NOT EXISTS monthly_income NUMERIC(12,2);
