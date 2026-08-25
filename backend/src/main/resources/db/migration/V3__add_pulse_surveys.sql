CREATE TABLE pulse_surveys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_code VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    union_unit_id BIGINT NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    target_responses INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pulse_survey_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT chk_pulse_survey_target CHECK (target_responses > 0),
    CONSTRAINT chk_pulse_survey_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_pulse_survey_unit_status ON pulse_surveys(union_unit_id, status);
CREATE INDEX idx_pulse_survey_period ON pulse_surveys(start_date, end_date);

CREATE TABLE pulse_survey_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    rating INT NOT NULL,
    need_category VARCHAR(120) NOT NULL,
    suggestion VARCHAR(2000),
    anonymous BOOLEAN NOT NULL DEFAULT TRUE,
    respondent_name VARCHAR(150),
    submitted_on DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pulse_response_survey FOREIGN KEY (survey_id) REFERENCES pulse_surveys(id) ON DELETE CASCADE,
    CONSTRAINT chk_pulse_response_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_pulse_response_survey ON pulse_survey_responses(survey_id);
CREATE INDEX idx_pulse_response_date ON pulse_survey_responses(submitted_on);

INSERT INTO pulse_surveys (survey_code, title, union_unit_id, question_text, start_date, end_date, status, target_responses)
VALUES
('KS-0826-VCS', 'Khảo sát kết nối tháng 8', 1, 'Bạn đánh giá mức độ kết nối và hỗ trợ tại nơi làm việc như thế nào?', '2026-08-01', '2026-09-30', 'ACTIVE', 4),
('KS-0826-GPL', 'Nhu cầu phúc lợi GPL', 2, 'Nhu cầu nào cần được công đoàn ưu tiên trong tháng tới?', '2026-08-01', '2026-08-20', 'CLOSED', 2);

INSERT INTO pulse_survey_responses (survey_id, rating, need_category, suggestion, anonymous, respondent_name, submitted_on)
VALUES
(1, 5, 'Sức khỏe tinh thần', 'Tổ chức thêm hoạt động kết nối liên phòng ban.', TRUE, NULL, '2026-08-12'),
(1, 4, 'Điều kiện làm việc', 'Cần cải thiện khu vực nghỉ giữa ca.', FALSE, 'Nguyễn Trần Hải Yến', '2026-08-15'),
(1, 3, 'Sức khỏe tinh thần', 'Mong có kênh tư vấn kín đáo.', TRUE, NULL, '2026-08-18'),
(2, 4, 'Phúc lợi', 'Ưu tiên chương trình chăm sóc gia đình.', TRUE, NULL, '2026-08-10');
