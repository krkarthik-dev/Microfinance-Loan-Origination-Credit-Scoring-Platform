-- =============================================================================
-- V12__make_profile_fields_nullable_for_signup.sql
-- Relax NOT NULL constraints to support basic signup capturing only Name.
-- =============================================================================

ALTER TABLE user_profiles
    ALTER COLUMN date_of_birth DROP NOT NULL,
    ALTER COLUMN gender DROP NOT NULL,
    ALTER COLUMN phone_number DROP NOT NULL,
    ALTER COLUMN address_line1 DROP NOT NULL,
    ALTER COLUMN city DROP NOT NULL,
    ALTER COLUMN state DROP NOT NULL,
    ALTER COLUMN pincode DROP NOT NULL;
