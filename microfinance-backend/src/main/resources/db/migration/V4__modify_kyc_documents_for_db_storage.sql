-- =============================================================================
-- V4__modify_kyc_documents_for_db_storage.sql
-- Modify kyc_documents table to store binary data directly in the database.
-- =============================================================================

ALTER TABLE kyc_documents 
DROP COLUMN s3_bucket,
DROP COLUMN s3_key;

ALTER TABLE kyc_documents 
ADD COLUMN file_data BYTEA NOT NULL,
ADD COLUMN content_type VARCHAR(100) NOT NULL;
