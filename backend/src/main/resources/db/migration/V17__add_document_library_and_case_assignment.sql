ALTER TABLE labor_cases MODIFY COLUMN owner_name VARCHAR(150) NULL;
ALTER TABLE labor_cases MODIFY COLUMN deadline DATE NULL;

CREATE TABLE document_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    category VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_library_unit FOREIGN KEY (union_unit_id)
        REFERENCES union_units(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_library_unit_created
    ON document_library(union_unit_id, created_at);
