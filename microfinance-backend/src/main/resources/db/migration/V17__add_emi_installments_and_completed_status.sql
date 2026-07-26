-- V17__add_emi_installments_and_completed_status.sql
-- Add terminal COMPLETED status and emi_installments repayment ledger table

ALTER TABLE loan_applications DROP CONSTRAINT chk_application_status;

ALTER TABLE loan_applications ADD CONSTRAINT chk_application_status CHECK (
    status IN (
        'DRAFT',
        'SUBMITTED',
        'PENDING_KYC',
        'UNDER_REVIEW',
        'INFO_REQUESTED',
        'PENDING_MANAGER_APPROVAL',
        'APPROVED',
        'CLOSING',
        'ACTIVE_REPAYMENT',
        'REJECTED',
        'WITHDRAWN',
        'COMPLETED',
        'CLOSED_PAID_IN_FULL'
    )
);

CREATE TABLE emi_installments (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES loan_applications(id),
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_amount NUMERIC(12, 2) NOT NULL,
    interest_amount NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    remaining_balance NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_date TIMESTAMP,
    payment_method VARCHAR(50),
    reference_number VARCHAR(100),
    collected_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_emi_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE'))
);

CREATE INDEX idx_emi_app_id ON emi_installments(application_id);
