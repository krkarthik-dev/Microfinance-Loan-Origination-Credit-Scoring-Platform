-- =============================================================================
-- V1__create_initial_schema.sql
-- Microfinance Loan Origination & Credit Scoring Platform
-- Initial schema creation — 8 tables normalized to 3NF
-- Managed by Flyway | PostgreSQL
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 1: users
-- Stores login credentials and platform role.
-- Personal details are intentionally separated into user_profiles (3NF).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id             BIGSERIAL       PRIMARY KEY,
    username       VARCHAR(50)     NOT NULL UNIQUE,
    email          VARCHAR(100)    NOT NULL UNIQUE,
    password_hash  VARCHAR(255)    NOT NULL,
    role           VARCHAR(20)     NOT NULL,
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_users_role CHECK (role IN ('ROLE_APPLICANT', 'ROLE_OFFICER', 'ROLE_ADMIN'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 2: user_profiles
-- Personal, address, and KYC identity data.
-- Separated from users: no transitive dependency between login and personal info.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_profiles (
    id               BIGSERIAL       PRIMARY KEY,
    user_id          BIGINT          NOT NULL UNIQUE,
    first_name       VARCHAR(50)     NOT NULL,
    last_name        VARCHAR(50)     NOT NULL,
    date_of_birth    DATE            NOT NULL,
    gender           VARCHAR(10)     NOT NULL,
    phone_number     VARCHAR(15)     NOT NULL UNIQUE,
    address_line1    VARCHAR(255)    NOT NULL,
    address_line2    VARCHAR(255),
    city             VARCHAR(100)    NOT NULL,
    state            VARCHAR(100)    NOT NULL,
    pincode          VARCHAR(10)     NOT NULL,
    pan_number       VARCHAR(10)     UNIQUE,
    aadhaar_number   VARCHAR(12)     UNIQUE,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_profile_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 3: loan_products
-- Master list of loan types offered by the institution.
-- Managed by admin. Referenced by all loan applications.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS loan_products (
    id                   BIGSERIAL       PRIMARY KEY,
    product_name         VARCHAR(100)    NOT NULL UNIQUE,
    description          TEXT,
    min_amount           NUMERIC(12,2)   NOT NULL,
    max_amount           NUMERIC(12,2)   NOT NULL,
    interest_rate_pa     NUMERIC(5,2)    NOT NULL,
    min_tenure_months    INTEGER         NOT NULL,
    max_tenure_months    INTEGER         NOT NULL,
    processing_fee_pct   NUMERIC(4,2)    NOT NULL DEFAULT 0.00,
    is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_product_amounts CHECK (max_amount >= min_amount),
    CONSTRAINT chk_product_tenure  CHECK (max_tenure_months >= min_tenure_months),
    CONSTRAINT chk_interest_rate   CHECK (interest_rate_pa > 0)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 4: loan_applications
-- Core entity. Every loan request follows the defined lifecycle states.
-- applicant_id and loan_officer_id both reference users (different roles).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS loan_applications (
    id                   BIGSERIAL       PRIMARY KEY,
    application_number   VARCHAR(20)     NOT NULL UNIQUE,
    applicant_id         BIGINT          NOT NULL,
    loan_officer_id      BIGINT,
    loan_product_id      BIGINT          NOT NULL,
    applied_amount       NUMERIC(12,2)   NOT NULL,
    approved_amount      NUMERIC(12,2),
    tenure_months        INTEGER         NOT NULL,
    purpose              TEXT            NOT NULL,
    status               VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    submitted_at         TIMESTAMP,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_application_applicant
        FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_application_officer
        FOREIGN KEY (loan_officer_id) REFERENCES users(id),
    CONSTRAINT fk_application_product
        FOREIGN KEY (loan_product_id) REFERENCES loan_products(id),
    CONSTRAINT chk_application_status CHECK (
        status IN (
            'DRAFT', 'SUBMITTED', 'RISK_ASSESSMENT', 'UNDER_REVIEW',
            'APPROVED', 'REJECTED', 'ESCALATED',
            'FINAL_APPROVED', 'FINAL_REJECTED'
        )
    ),
    CONSTRAINT chk_applied_amount CHECK (applied_amount > 0)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 5: credit_scores
-- ML model output per loan application (1:1).
-- Populated asynchronously after submission via Spring Event.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS credit_scores (
    id                       BIGSERIAL       PRIMARY KEY,
    application_id           BIGINT          NOT NULL UNIQUE,
    credit_score             INTEGER         NOT NULL,
    probability_of_default   NUMERIC(5,4)    NOT NULL,
    risk_tier                VARCHAR(10)     NOT NULL,
    model_version            VARCHAR(20)     NOT NULL,
    scored_at                TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_credit_application
        FOREIGN KEY (application_id) REFERENCES loan_applications(id),
    CONSTRAINT chk_credit_score_range
        CHECK (credit_score BETWEEN 300 AND 900),
    CONSTRAINT chk_pod_range
        CHECK (probability_of_default BETWEEN 0 AND 1),
    CONSTRAINT chk_risk_tier
        CHECK (risk_tier IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 6: loan_decisions
-- Records each decision event (approve/reject/escalate).
-- One application can have multiple decision records over its lifecycle.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS loan_decisions (
    id               BIGSERIAL       PRIMARY KEY,
    application_id   BIGINT          NOT NULL,
    decided_by       BIGINT          NOT NULL,
    decision         VARCHAR(20)     NOT NULL,
    remarks          TEXT,
    decided_at       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_decision_application
        FOREIGN KEY (application_id) REFERENCES loan_applications(id),
    CONSTRAINT fk_decision_officer
        FOREIGN KEY (decided_by) REFERENCES users(id),
    CONSTRAINT chk_decision_type
        CHECK (decision IN ('APPROVED', 'REJECTED', 'ESCALATED'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 7: kyc_documents
-- S3 references for Aadhaar and PAN documents.
-- Actual files are stored in AWS S3; only metadata is stored here.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS kyc_documents (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL,
    document_type     VARCHAR(20)     NOT NULL,
    s3_bucket         VARCHAR(100)    NOT NULL,
    s3_key            VARCHAR(255)    NOT NULL,
    file_name         VARCHAR(255)    NOT NULL,
    file_size_bytes   BIGINT,
    is_verified       BOOLEAN         NOT NULL DEFAULT FALSE,
    verified_by       BIGINT,
    uploaded_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    verified_at       TIMESTAMP,

    CONSTRAINT fk_kyc_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_kyc_verifier
        FOREIGN KEY (verified_by) REFERENCES users(id),
    CONSTRAINT chk_document_type
        CHECK (document_type IN ('AADHAAR', 'PAN'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- TABLE 8: audit_logs
-- Immutable append-only event trail. Never updated or deleted.
-- performed_by is nullable to allow SYSTEM-generated events.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGSERIAL       PRIMARY KEY,
    entity_type    VARCHAR(50)     NOT NULL,
    entity_id      BIGINT          NOT NULL,
    action         VARCHAR(50)     NOT NULL,
    performed_by   BIGINT,
    old_value      TEXT,
    new_value      TEXT,
    ip_address     VARCHAR(45),
    created_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_user
        FOREIGN KEY (performed_by) REFERENCES users(id)
);
