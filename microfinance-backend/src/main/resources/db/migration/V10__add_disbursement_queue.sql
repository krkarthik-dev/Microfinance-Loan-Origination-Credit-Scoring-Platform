CREATE TABLE disbursement_queue (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES loan_applications(id),
    approved_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    queued_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);
