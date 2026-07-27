-- V19__create_loan_id_sequence_table.sql
-- Create loan_id_sequence table to support thread-safe atomic sequence generation for US64

CREATE TABLE loan_id_sequence (
    sequence_key VARCHAR(10) PRIMARY KEY,
    next_val BIGINT NOT NULL DEFAULT 1
);
