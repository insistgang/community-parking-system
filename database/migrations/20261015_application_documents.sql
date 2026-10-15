-- MySQL 8: add private application-document metadata without altering existing data.
CREATE TABLE IF NOT EXISTS application_documents (
    id VARCHAR(36) NOT NULL,
    owner_id INT NOT NULL,
    community_id INT NOT NULL,
    application_id INT NULL,
    document_type VARCHAR(32) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_document_owner (owner_id),
    KEY idx_document_application (application_id),
    CONSTRAINT fk_document_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_document_community FOREIGN KEY (community_id) REFERENCES communities (id),
    CONSTRAINT fk_document_application FOREIGN KEY (application_id) REFERENCES queue_applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
