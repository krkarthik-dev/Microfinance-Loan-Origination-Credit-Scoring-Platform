-- V13__update_application_status_constraint.sql
-- Updates the application status enum states to the new industry-standard flow

-- 1. Migrate existing old data to closest new equivalents
UPDATE loan_applications SET status = 'UNDER_REVIEW' WHERE status = 'RISK_ASSESSMENT';
UPDATE loan_applications SET status = 'PENDING_MANAGER_APPROVAL' WHERE status = 'ESCALATED';
UPDATE loan_applications SET status = 'APPROVED' WHERE status = 'FINAL_APPROVED';
UPDATE loan_applications SET status = 'REJECTED' WHERE status = 'FINAL_REJECTED';
UPDATE loan_applications SET status = 'ACTIVE_REPAYMENT' WHERE status = 'DISBURSEMENT';
UPDATE loan_applications SET status = 'ACTIVE_REPAYMENT' WHERE status = 'ACTIVE';

-- 2. Drop the old constraint
ALTER TABLE loan_applications DROP CONSTRAINT chk_application_status;

-- 3. Add the new constraint with refined statuses
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
        'WITHDRAWN'
    )
);
