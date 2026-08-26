ALTER TABLE labor_cases ADD COLUMN requester_name VARCHAR(150) NOT NULL DEFAULT 'Chưa cập nhật';
ALTER TABLE labor_cases ADD COLUMN source VARCHAR(120);
ALTER TABLE labor_cases ADD COLUMN attachment_note VARCHAR(500);

ALTER TABLE welfare_records ADD COLUMN policy_name VARCHAR(180);
ALTER TABLE welfare_records ADD COLUMN standard_amount DECIMAL(15,2);
ALTER TABLE welfare_records ADD COLUMN deadline DATE;
ALTER TABLE welfare_records ADD COLUMN receipt_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE';
ALTER TABLE welfare_records ADD COLUMN has_image BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE union_activities ADD COLUMN participant_list VARCHAR(2000);
ALTER TABLE union_activities ADD COLUMN check_in_count INT NOT NULL DEFAULT 0;
ALTER TABLE union_activities ADD COLUMN quick_feedback VARCHAR(2000);
ALTER TABLE union_activities ADD COLUMN issues VARCHAR(2000);
ALTER TABLE union_activities ADD COLUMN document_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE';
ALTER TABLE union_activities ADD COLUMN lessons_learned VARCHAR(2000);

CREATE TABLE member_changes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    change_type VARCHAR(120) NOT NULL,
    effective_date DATE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    recorded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_change_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

CREATE INDEX idx_member_changes_member_date ON member_changes(member_id, effective_date);

CREATE TABLE member_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_document_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

CREATE INDEX idx_member_documents_member_type ON member_documents(member_id, document_type);

CREATE TABLE activity_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    title VARCHAR(180),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_media_activity FOREIGN KEY (activity_id) REFERENCES union_activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_media_activity_type ON activity_media(activity_id, media_type);
