ALTER TABLE loan_correction_requests
ADD COLUMN created_by VARCHAR(255),
ADD COLUMN updated_by VARCHAR(255);
