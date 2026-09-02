CREATE TABLE labor_case_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    labor_case_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_labor_case_document_case FOREIGN KEY (labor_case_id)
        REFERENCES labor_cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_labor_case_documents_case_created
    ON labor_case_documents(labor_case_id, created_at);
