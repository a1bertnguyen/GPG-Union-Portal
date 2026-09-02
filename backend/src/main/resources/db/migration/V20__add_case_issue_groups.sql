CREATE TABLE case_issue_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO case_issue_groups (code, name, active) VALUES
    ('LUONG_THUONG', 'Lương, thưởng và phúc lợi', TRUE),
    ('DIEU_KIEN_LAM_VIEC', 'Điều kiện làm việc', TRUE),
    ('QUAN_HE_LAO_DONG', 'Quan hệ lao động', TRUE),
    ('AN_TOAN_LAO_DONG', 'An toàn, vệ sinh lao động', TRUE),
    ('KHAC', 'Khác', TRUE);
