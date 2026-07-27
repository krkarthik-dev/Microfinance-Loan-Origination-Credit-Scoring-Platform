-- V18__add_auditing_to_emi_installments.sql
-- Add created_by and last_modified_by auditing columns for BaseEntity inheritance in EmiInstallment

ALTER TABLE emi_installments
ADD COLUMN created_by VARCHAR(50),
ADD COLUMN last_modified_by VARCHAR(50);
