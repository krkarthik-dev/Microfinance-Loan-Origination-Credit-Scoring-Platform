-- V5: Add guarantor columns, loan_documents table, signature and PDF storage on loan_applications

-- 1. Add guarantor & submission columns to loan_applications
ALTER TABLE loan_applications
    ADD COLUMN IF NOT EXISTS guarantor_name        VARCHAR(200),
    ADD COLUMN IF NOT EXISTS guarantor_address     TEXT,
    ADD COLUMN IF NOT EXISTS guarantor_city        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS guarantor_zip         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS guarantor_aadhaar     VARCHAR(12),
    ADD COLUMN IF NOT EXISTS guarantor_pan         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS signature_image       BYTEA,
    ADD COLUMN IF NOT EXISTS signature_content_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS application_pdf       BYTEA,
    ADD COLUMN IF NOT EXISTS terms_accepted        BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Create loan_documents table (income cert, photo, guarantor ID, others)
CREATE TABLE IF NOT EXISTS loan_documents (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT        NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    document_type   VARCHAR(50)   NOT NULL,   -- INCOME_CERTIFICATE | PHOTOGRAPH | GUARANTOR_ID | OTHER
    file_name       VARCHAR(255)  NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    file_data       BYTEA         NOT NULL,
    uploaded_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_loan_documents_app ON loan_documents(application_id);
