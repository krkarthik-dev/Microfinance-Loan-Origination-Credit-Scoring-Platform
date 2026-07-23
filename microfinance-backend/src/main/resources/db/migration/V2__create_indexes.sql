-- =============================================================================
-- V2__create_indexes.sql
-- Performance indexes for frequently queried and joined columns
-- =============================================================================

-- ── users ────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_users_email  ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role   ON users(role);

-- ── user_profiles ────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_profiles_user_id  ON user_profiles(user_id);

-- ── loan_applications ────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_applications_applicant   ON loan_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_applications_officer     ON loan_applications(loan_officer_id);
CREATE INDEX IF NOT EXISTS idx_applications_status      ON loan_applications(status);
CREATE INDEX IF NOT EXISTS idx_applications_product     ON loan_applications(loan_product_id);
CREATE INDEX IF NOT EXISTS idx_applications_number      ON loan_applications(application_number);

-- ── credit_scores ────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_credit_application  ON credit_scores(application_id);
CREATE INDEX IF NOT EXISTS idx_credit_risk_tier    ON credit_scores(risk_tier);

-- ── loan_decisions ───────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_decisions_application  ON loan_decisions(application_id);
CREATE INDEX IF NOT EXISTS idx_decisions_decided_by   ON loan_decisions(decided_by);

-- ── kyc_documents ────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_kyc_user_id        ON kyc_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_kyc_document_type  ON kyc_documents(document_type);

-- ── audit_logs ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_audit_entity        ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by  ON audit_logs(performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_created_at    ON audit_logs(created_at DESC);
