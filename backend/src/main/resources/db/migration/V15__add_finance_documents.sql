CREATE TABLE finance_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finance_entry_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_finance_document_entry FOREIGN KEY (finance_entry_id)
        REFERENCES finance_entries(id) ON DELETE CASCADE
);

CREATE INDEX idx_finance_documents_entry
    ON finance_documents(finance_entry_id);
