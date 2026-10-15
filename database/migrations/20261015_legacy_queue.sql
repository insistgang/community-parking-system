-- Apply once after a database backup, before enabling legacy queue imports.
-- Preflight: these columns must be absent; existing queue rows remain unchanged.
ALTER TABLE queue_applications
    ADD COLUMN legacy_applicant_name VARCHAR(255) NULL,
    ADD COLUMN legacy_applicant_phone VARCHAR(255) NULL,
    ADD COLUMN legacy_id_card VARCHAR(255) NULL,
    ADD COLUMN legacy_car_relationship VARCHAR(255) NULL,
    ADD COLUMN legacy_priority_text VARCHAR(255) NULL,
    ADD COLUMN legacy_source_rank INT NULL,
    ADD COLUMN legacy_apply_time_text VARCHAR(255) NULL,
    ADD COLUMN legacy_notes TEXT NULL,
    ADD COLUMN legacy_import_batch_id VARCHAR(64) NULL,
    ADD COLUMN legacy_source_row INT NULL,
    ADD CONSTRAINT uk_queue_import_source UNIQUE (legacy_import_batch_id, legacy_source_row);
ALTER TABLE users ADD COLUMN job_title VARCHAR(255) NULL, ADD COLUMN notes TEXT NULL;
ALTER TABLE communities ADD COLUMN source_audit_mode VARCHAR(255) NULL;
ALTER TABLE import_batches ADD COLUMN queue_count INT NOT NULL DEFAULT 0;
