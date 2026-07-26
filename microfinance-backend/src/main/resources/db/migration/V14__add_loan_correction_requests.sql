CREATE TABLE loan_correction_requests (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    section VARCHAR(255) NOT NULL,
    comments TEXT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_loan_correction_req_app FOREIGN KEY (application_id) REFERENCES loan_applications(id) ON DELETE CASCADE
);
