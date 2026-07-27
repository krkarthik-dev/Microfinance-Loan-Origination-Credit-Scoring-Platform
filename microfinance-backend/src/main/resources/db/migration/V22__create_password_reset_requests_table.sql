-- V22: Create password_reset_requests table for Forgot Password flow
CREATE TABLE password_reset_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    temp_password VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_password_resets_status ON password_reset_requests(status);
CREATE INDEX idx_password_resets_request_id ON password_reset_requests(request_id);
