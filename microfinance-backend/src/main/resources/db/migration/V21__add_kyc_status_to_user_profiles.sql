-- V21__add_kyc_status_to_user_profiles.sql

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(50) NOT NULL DEFAULT 'MISSING';
