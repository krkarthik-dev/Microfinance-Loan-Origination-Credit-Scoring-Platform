-- V9__add_kyc_verified.sql

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS kyc_verified BOOLEAN NOT NULL DEFAULT FALSE;
