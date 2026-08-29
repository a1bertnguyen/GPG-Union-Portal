CREATE TABLE welfare_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    welfare_record_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_welfare_document_record FOREIGN KEY (welfare_record_id)
        REFERENCES welfare_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_welfare_documents_record_type
    ON welfare_documents(welfare_record_id, document_type);
