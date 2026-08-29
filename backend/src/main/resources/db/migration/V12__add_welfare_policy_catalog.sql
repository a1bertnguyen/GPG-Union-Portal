CREATE TABLE welfare_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    source VARCHAR(20) NOT NULL,
    sequence_number INT NOT NULL,
    welfare_type VARCHAR(30) NOT NULL,
    name VARCHAR(180) NOT NULL,
    support_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    eligibility_notes VARCHAR(1000),
    processing_weeks INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_welfare_policy_weeks CHECK (processing_weeks BETWEEN 1 AND 8)
);

ALTER TABLE welfare_records ADD COLUMN policy_id BIGINT;
ALTER TABLE welfare_records ADD CONSTRAINT fk_welfare_record_policy
    FOREIGN KEY (policy_id) REFERENCES welfare_policies(id);

CREATE INDEX idx_welfare_policies_active_type ON welfare_policies(active, welfare_type);
CREATE INDEX idx_welfare_records_policy ON welfare_records(policy_id);

INSERT INTO welfare_policies
    (code, source, sequence_number, welfare_type, name, support_amount, eligibility_notes, processing_weeks, active)
VALUES
    ('CD-01-01', 'UNION', 1, 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', 300000, 'Tất cả NV công ty (Nhân viên thử việc và nhân viên chính thức)', 1, TRUE),
    ('CD-02-01', 'UNION', 2, 'WEDDING', 'Đám cưới', 300000, 'Tất cả NV công ty (Nhân viên thử việc và nhân viên chính thức)', 1, TRUE),
    ('CD-03-01', 'UNION', 3, 'BIRTHDAY', 'Sinh Nhật', 100000, NULL, 1, TRUE),
    ('CD-04-01', 'UNION', 4, 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện', 300000, 'Nhân viên chính thức', 1, TRUE),
    ('CD-05-01', 'UNION', 5, 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện phẫu thuật', 1000000, 'Nhân viên chính thức', 1, TRUE),
    ('CD-06-01', 'UNION', 6, 'CHILDBIRTH', 'Nhân viên nữ sinh con', 1000000, 'Nhân viên làm từ 3 năm', 1, TRUE),
    ('CD-07-01', 'UNION', 7, 'CHILDBIRTH', 'Nhân viên nữ sinh con', 500000, 'Nhân viên làm từ 2 năm', 1, TRUE),
    ('CD-08-01', 'UNION', 8, 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', 500000, 'Nhân viên làm từ 2 năm', 1, TRUE),
    ('CD-09-01', 'UNION', 9, 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', 200000, 'Nhân viên làm từ 1 năm', 1, TRUE),
    ('CT-10-01', 'COMPANY', 10, 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', 1000000, 'NV làm trên 1 năm', 1, TRUE),
    ('CT-10-02', 'COMPANY', 10, 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', 500000, 'NV làm dưới 6 tháng', 1, TRUE),
    ('CT-11-01', 'COMPANY', 11, 'WEDDING', 'Đám cưới', 700000, 'NV làm trên 6 tháng', 1, TRUE),
    ('CT-11-02', 'COMPANY', 11, 'WEDDING', 'Đám cưới', 400000, 'NV làm dưới 6 tháng', 1, TRUE),
    ('CT-12-01', 'COMPANY', 12, 'CHILDBIRTH', 'Nhân viên nữ sinh con', 1000000, 'Nhân viên làm từ 3 năm', 1, TRUE),
    ('CT-13-01', 'COMPANY', 13, 'CHILDBIRTH', 'Nhân viên nữ sinh con', 500000, 'Nhân viên làm từ 2 năm', 1, TRUE),
    ('CT-14-01', 'COMPANY', 14, 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', 500000, 'Nhân viên làm từ 2 năm', 1, TRUE),
    ('CT-15-01', 'COMPANY', 15, 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', 300000, 'Nhân viên làm từ 1 năm', 1, TRUE);
