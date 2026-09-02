CREATE TABLE kpi_versions (
    version_id VARCHAR(60) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(30) NOT NULL,
    score_scale DECIMAL(8,4) NOT NULL,
    round_display INT NOT NULL,
    bonus_cap DECIMAL(8,4) NOT NULL,
    data_quality_final_threshold DECIMAL(8,6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    approved_by VARCHAR(150) NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kpi_classification_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id VARCHAR(60) NOT NULL,
    minimum_score DECIMAL(8,4) NOT NULL,
    label VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT uq_kpi_classification_version_label UNIQUE (version_id, label),
    CONSTRAINT fk_kpi_classification_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE kpi_classification_gates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id VARCHAR(60) NOT NULL,
    gate_code VARCHAR(60) NOT NULL,
    classification_cap VARCHAR(40) NOT NULL,
    detection_rule VARCHAR(1000) NOT NULL,
    CONSTRAINT uq_kpi_gate_version_code UNIQUE (version_id, gate_code),
    CONSTRAINT fk_kpi_gate_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE kpi_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id VARCHAR(60) NOT NULL,
    kpi_code VARCHAR(20) NOT NULL,
    group_code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    weight DECIMAL(8,4) NOT NULL,
    direction VARCHAR(30) NOT NULL,
    target_value DECIMAL(12,6) NULL,
    max_allowed_value DECIMAL(12,6) NULL,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    na_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    source_module VARCHAR(50) NOT NULL,
    numerator_rule VARCHAR(1000) NOT NULL,
    denominator_rule VARCHAR(1000) NOT NULL,
    evidence_rule VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kpi_definition_version_code UNIQUE (version_id, kpi_code),
    CONSTRAINT fk_kpi_definition_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE sla_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sla_code VARCHAR(60) NOT NULL,
    version_id VARCHAR(60) NOT NULL,
    case_type VARCHAR(60) NOT NULL,
    priority VARCHAR(40) NULL,
    duration_value INT NOT NULL,
    duration_unit VARCHAR(30) NOT NULL,
    business_calendar_id VARCHAR(60) NOT NULL,
    CONSTRAINT uq_sla_rule_version_code UNIQUE (version_id, sla_code),
    CONSTRAINT fk_sla_rule_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE business_calendar_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_calendar_id VARCHAR(60) NOT NULL,
    calendar_date DATE NOT NULL,
    working_day BOOLEAN NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT uq_business_calendar_day UNIQUE (business_calendar_id, calendar_date)
);

CREATE TABLE penalty_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    penalty_code VARCHAR(20) NOT NULL,
    version_id VARCHAR(60) NOT NULL,
    points_per_case DECIMAL(8,4) NOT NULL,
    period_cap DECIMAL(8,4) NULL,
    classification_cap VARCHAR(40) NULL,
    detection_rule VARCHAR(1000) NOT NULL,
    CONSTRAINT uq_penalty_rule_version_code UNIQUE (version_id, penalty_code),
    CONSTRAINT fk_penalty_rule_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE kpi_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_key VARCHAR(160) NOT NULL,
    union_unit_id BIGINT NOT NULL,
    period_type VARCHAR(30) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    version_id VARCHAR(60) NOT NULL,
    revision INT NOT NULL DEFAULT 1,
    cutoff_at TIMESTAMP NOT NULL,
    run_status VARCHAR(30) NOT NULL,
    data_quality_rate DECIMAL(12,8) NOT NULL,
    base_score DECIMAL(12,8) NOT NULL,
    bonus_points DECIMAL(12,8) NOT NULL,
    penalty_points DECIMAL(12,8) NOT NULL,
    final_score DECIMAL(12,8) NOT NULL,
    raw_classification VARCHAR(40) NOT NULL,
    final_classification VARCHAR(40) NOT NULL,
    ranking_position INT NULL,
    input_hash VARCHAR(64) NOT NULL,
    previous_run_id BIGINT NULL,
    calculated_at TIMESTAMP NOT NULL,
    calculated_by VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kpi_run_revision UNIQUE (union_unit_id, period_type, period_start, period_end, version_id, revision),
    CONSTRAINT uq_kpi_run_key UNIQUE (run_key),
    CONSTRAINT fk_kpi_run_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT fk_kpi_run_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id),
    CONSTRAINT fk_kpi_run_previous FOREIGN KEY (previous_run_id) REFERENCES kpi_runs(id)
);

CREATE TABLE kpi_result_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    kpi_code VARCHAR(20) NOT NULL,
    numerator DECIMAL(18,6) NULL,
    denominator DECIMAL(18,6) NULL,
    target_value DECIMAL(12,6) NULL,
    normalized_score DECIMAL(12,8) NULL,
    eligible_weight DECIMAL(8,4) NOT NULL,
    earned_points DECIMAL(12,8) NOT NULL,
    result_status VARCHAR(30) NOT NULL,
    explanation VARCHAR(2000) NOT NULL,
    CONSTRAINT uq_kpi_result_run_code UNIQUE (run_id, kpi_code),
    CONSTRAINT fk_kpi_result_run FOREIGN KEY (run_id) REFERENCES kpi_runs(id)
);

CREATE TABLE kpi_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    source_record_id VARCHAR(100) NOT NULL,
    evidence_role VARCHAR(30) NOT NULL,
    evidence_url VARCHAR(1000) NULL,
    validation_status VARCHAR(30) NOT NULL,
    redacted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_kpi_evidence_result FOREIGN KEY (result_id) REFERENCES kpi_result_details(id)
);

CREATE TABLE kpi_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NULL,
    union_unit_id BIGINT NOT NULL,
    period_type VARCHAR(30) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    version_id VARCHAR(60) NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    penalty_code VARCHAR(20) NULL,
    points DECIMAL(8,4) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    evidence_module VARCHAR(50) NULL,
    evidence_record_id VARCHAR(100) NULL,
    effectiveness_verified BOOLEAN NOT NULL DEFAULT FALSE,
    non_duplicate_verified BOOLEAN NOT NULL DEFAULT FALSE,
    requested_by VARCHAR(150) NOT NULL,
    approved_by VARCHAR(150) NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kpi_adjustment_run FOREIGN KEY (run_id) REFERENCES kpi_runs(id),
    CONSTRAINT fk_kpi_adjustment_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT fk_kpi_adjustment_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id),
    CONSTRAINT ck_kpi_adjustment_points_non_negative CHECK (points >= 0),
    CONSTRAINT ck_kpi_adjustment_type CHECK (adjustment_type IN ('BONUS', 'PENALTY')),
    CONSTRAINT ck_kpi_approved_adjustment_evidence CHECK (
        approved_at IS NULL OR (evidence_module IS NOT NULL AND evidence_record_id IS NOT NULL)
    ),
    CONSTRAINT ck_kpi_approved_bonus_validation CHECK (
        approved_at IS NULL OR adjustment_type <> 'BONUS'
        OR (effectiveness_verified = TRUE AND non_duplicate_verified = TRUE)
    )
);

CREATE TABLE kpi_no_occurrence_confirmations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    union_unit_id BIGINT NOT NULL,
    version_id VARCHAR(60) NOT NULL,
    kpi_code VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    reconciliation_source_module VARCHAR(50) NOT NULL,
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_by VARCHAR(150) NOT NULL,
    approved_by VARCHAR(150) NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kpi_no_occurrence UNIQUE (union_unit_id, version_id, kpi_code, period_start, period_end),
    CONSTRAINT fk_kpi_no_occurrence_unit FOREIGN KEY (union_unit_id) REFERENCES union_units(id),
    CONSTRAINT fk_kpi_no_occurrence_version FOREIGN KEY (version_id) REFERENCES kpi_versions(version_id)
);

CREATE TABLE kpi_source_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    source_record_id VARCHAR(100) NOT NULL,
    source_updated_at TIMESTAMP NULL,
    payload_hash VARCHAR(64) NOT NULL,
    snapshot_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kpi_snapshot_record UNIQUE (run_id, source_module, source_record_id),
    CONSTRAINT fk_kpi_snapshot_run FOREIGN KEY (run_id) REFERENCES kpi_runs(id)
);

CREATE TABLE kpi_run_warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    warning_code VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    recommended_action VARCHAR(1000) NULL,
    source_module VARCHAR(50) NULL,
    source_record_id VARCHAR(100) NULL,
    redacted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_kpi_warning_run FOREIGN KEY (run_id) REFERENCES kpi_runs(id)
);

CREATE TABLE kpi_source_exclusions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_module VARCHAR(50) NOT NULL,
    source_record_key VARCHAR(160) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kpi_source_exclusion UNIQUE (source_module, source_record_key)
);

INSERT INTO kpi_versions(version_id, name, effective_from, status, score_scale, round_display,
                         bonus_cap, data_quality_final_threshold, created_by, approved_by, approved_at)
VALUES ('GPG-CD-KPI-V1', 'KPI Công đoàn cơ sở GPG V1', '2026-01-01', 'ACTIVE', 100, 2,
        3, 0.80, 'system', 'system', CURRENT_TIMESTAMP);

INSERT INTO kpi_classification_rules(version_id, minimum_score, label, sort_order)
VALUES
('GPG-CD-KPI-V1',90,'Xuất sắc',1),
('GPG-CD-KPI-V1',80,'Tốt',2),
('GPG-CD-KPI-V1',65,'Khá',3),
('GPG-CD-KPI-V1',50,'Trung bình',4),
('GPG-CD-KPI-V1',0,'Không đạt',5);

INSERT INTO kpi_classification_gates(version_id, gate_code, classification_cap, detection_rule)
VALUES
('GPG-CD-KPI-V1','INTEGRITY_VIOLATION','Không đạt','approved_penalty_P06_or_P07'),
('GPG-CD-KPI-V1','SERIOUS_OPEN_CASE','Trung bình','critical_grievance_or_overdue_care_open'),
('GPG-CD-KPI-V1','MISSING_MANDATORY_REPORT','Khá','mandatory_report_not_submitted'),
('GPG-CD-KPI-V1','GOVERNANCE_INCOMPLETE','Khá','required_legal_or_bch_profile_incomplete');

INSERT INTO kpi_definitions
    (version_id, kpi_code, group_code, name, weight, direction, target_value, max_allowed_value,
     mandatory, na_allowed, source_module, numerator_rule, denominator_rule, evidence_rule)
VALUES
('GPG-CD-KPI-V1','GOV01','GOV','Hồ sơ thành lập/kiện toàn hợp lệ',5,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'DM_CONG_DOAN','approved_valid_required_documents','required_documents','union_and_document_ids'),
('GPG-CD-KPI-V1','GOV02','GOV','BCH và phân công nhiệm vụ đầy đủ',4,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'DM_NHAN_SU_CD','valid_filled_assignments','required_positions','staff_assignment_ids'),
('GPG-CD-KPI-V1','GOV03','GOV','Hoàn thành tập huấn bắt buộc',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'DM_NHAN_SU_CD','completed_required_training','staff_required_training','training_completion_ids'),
('GPG-CD-KPI-V1','GOV04','GOV','Cập nhật thay đổi tổ chức đúng hạn',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'DM_CONG_DOAN','changes_updated_in_sla','organisation_changes','organisation_change_ids'),
('GPG-CD-KPI-V1','DATA01','DATA','Hồ sơ đoàn viên đầy đủ',5,'HIGHER_BETTER',1,NULL,FALSE,FALSE,'DOAN_VIEN','active_members_complete','active_members','membership_ids'),
('GPG-CD-KPI-V1','DATA02','DATA','Biến động đoàn viên cập nhật đúng hạn',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'DOAN_VIEN','hr_changes_recorded_in_sla','reconciled_hr_changes','member_change_and_hr_ids'),
('GPG-CD-KPI-V1','DATA03','DATA','Dữ liệu đúng và không trùng',3,'HIGHER_BETTER',1,NULL,FALSE,FALSE,'DOAN_VIEN','checked_records_without_error_or_duplicate','checked_records','validation_finding_ids'),
('GPG-CD-KPI-V1','DATA04','DATA','Tỷ lệ tham gia Công đoàn',3,'HIGHER_BETTER',0.80,NULL,FALSE,FALSE,'DOAN_VIEN','active_members','eligible_hr_employees','membership_and_hr_ids'),
('GPG-CD-KPI-V1','REP01','REP','Báo cáo định kỳ nộp đúng hạn',6,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'BAO_CAO_DINH_KY','reports_submitted_on_time','reports_due','report_ids'),
('GPG-CD-KPI-V1','REP02','REP','Báo cáo đầy đủ và khớp dữ liệu nguồn',4,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'BAO_CAO_DINH_KY','valid_reconciled_fields','fields_required','report_reconciliation_ids'),
('GPG-CD-KPI-V1','REP03','REP','Kế hoạch kỳ sau được lập và duyệt',3,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'KE_HOACH','approved_complete_plans','plans_due','plan_ids'),
('GPG-CD-KPI-V1','REP04','REP','Tỷ lệ hồ sơ bị trả bổ sung',2,'LOWER_BETTER',0,1,TRUE,FALSE,'PHEDUYET_LOG','returned_submissions','submitted_for_approval','approval_log_ids'),
('GPG-CD-KPI-V1','CARE01','CARE','Tiếp nhận/xác minh đúng hạn',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','verified_in_sla','care_cases_occurred','care_case_ids'),
('GPG-CD-KPI-V1','CARE02','CARE','Thực hiện chính sách đúng hạn',6,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','eligible_cases_supported_in_sla','eligible_cases','care_case_ids'),
('GPG-CD-KPI-V1','CARE03','CARE','Hồ sơ chăm lo khép kín',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','closed_loop_cases','care_cases','care_and_document_ids'),
('GPG-CD-KPI-V1','CARE04','CARE','Đúng đối tượng, điều kiện và mức hỗ trợ',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','policy_compliant_checked_cases','checked_cases','care_policy_ids'),
('GPG-CD-KPI-V1','CARE05','CARE','NLĐ xác nhận và hài lòng',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','confirmed_and_satisfied_cases','valid_feedback_cases','care_feedback_ids'),
('GPG-CD-KPI-V1','GRV01','GRV','Xác nhận tiếp nhận đúng SLA',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','acknowledged_in_sla','grievances_received','grievance_ids'),
('GPG-CD-KPI-V1','GRV02','GRV','Xử lý đúng SLA',5,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','completed_in_sla','cases_due_in_period','grievance_ids'),
('GPG-CD-KPI-V1','GRV03','GRV','Hồ sơ được đóng đúng quy trình',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','properly_closed_cases','closed_cases','grievance_and_document_ids'),
('GPG-CD-KPI-V1','GRV04','GRV','NLĐ đồng ý với kết quả',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','employees_agree','valid_feedback','grievance_feedback_ids'),
('GPG-CD-KPI-V1','GRV05','GRV','Mức độ hài lòng sau xử lý',2,'RATING_1_5',NULL,NULL,FALSE,TRUE,'SO_KIEN_NGHI','average_valid_rating','valid_feedback','grievance_feedback_ids'),
('GPG-CD-KPI-V1','ACT01','ACT','Hoàn thành chương trình theo kế hoạch',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','completed_activities','approved_activities_due','activity_ids'),
('GPG-CD-KPI-V1','ACT02','ACT','Tỷ lệ tham gia',2,'HIGHER_BETTER',0.80,NULL,FALSE,TRUE,'HOAT_DONG','actual_participants','planned_participants','activity_ids'),
('GPG-CD-KPI-V1','ACT03','ACT','Hoàn thành mục tiêu chương trình',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','goals_achieved','approved_goals','activity_goal_ids'),
('GPG-CD-KPI-V1','ACT04','ACT','Báo cáo sau chương trình đúng hạn, đủ hồ sơ',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'BAO_CAO_SAU_CT','valid_on_time_post_reports','programs_requiring_report','activity_and_report_ids'),
('GPG-CD-KPI-V1','ACT05','ACT','Điểm hài lòng chương trình',1,'RATING_1_5',NULL,NULL,FALSE,TRUE,'BAO_CAO_SAU_CT','average_valid_rating','valid_activity_feedback','activity_feedback_ids'),
('GPG-CD-KPI-V1','FIN01','FIN','Thu/chi có hồ sơ và chứng từ hợp lệ',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'TAI_CHINH_CD','transactions_with_valid_documents','transactions_requiring_documents','transaction_and_document_ids'),
('GPG-CD-KPI-V1','FIN02','FIN','Hạch toán và thanh toán đúng hạn',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'TAI_CHINH_CD','transactions_completed_in_sla','transactions_due','transaction_ids'),
('GPG-CD-KPI-V1','FIN03','FIN','Đối soát số dư chính xác',2,'BOOLEAN',1,NULL,FALSE,FALSE,'TAI_CHINH_CD','matched_reconciliations','reconciliations_due','reconciliation_ids'),
('GPG-CD-KPI-V1','FIN04','FIN','Tuân thủ ngân sách được duyệt',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'TAI_CHINH_CD','transactions_within_adjusted_budget','budget_controlled_transactions','transaction_plan_adjustment_ids');

INSERT INTO sla_rules(sla_code, version_id, case_type, priority, duration_value, duration_unit, business_calendar_id)
VALUES
('GRV_ACK','GPG-CD-KPI-V1','GRIEVANCE','ALL',1,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_ASSIGN','GPG-CD-KPI-V1','GRIEVANCE','ALL',2,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_NORMAL','GPG-CD-KPI-V1','GRIEVANCE','NORMAL',7,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_COMPLEX','GPG-CD-KPI-V1','GRIEVANCE','COMPLEX',15,'BUSINESS_DAY','GPG_DEFAULT'),
('CARE_URGENT','GPG-CD-KPI-V1','CARE','URGENT',1,'BUSINESS_DAY','GPG_DEFAULT'),
('CARE_NORMAL','GPG-CD-KPI-V1','CARE','NORMAL',3,'BUSINESS_DAY','GPG_DEFAULT'),
('ACT_POST_REPORT','GPG-CD-KPI-V1','ACTIVITY','ALL',5,'BUSINESS_DAY','GPG_DEFAULT'),
('MEMBER_CHANGE','GPG-CD-KPI-V1','MEMBER_CHANGE','ALL',5,'BUSINESS_DAY','GPG_DEFAULT');

-- The monthly-report cutoff calendar is intentionally not seeded. Section 17 of the
-- specification requires this calendar to be formally approved before REP01/P01 can run.

INSERT INTO penalty_rules(penalty_code, version_id, points_per_case, period_cap, classification_cap, detection_rule)
VALUES
('P01','GPG-CD-KPI-V1',5,15,'Khá','mandatory_report_not_submitted'),
('P02','GPG-CD-KPI-V1',2,10,'Trung bình','seriously_overdue_case'),
('P03','GPG-CD-KPI-V1',5,15,NULL,'wrong_care_target_or_policy'),
('P04','GPG-CD-KPI-V1',3,12,NULL,'finance_document_missing_after_due'),
('P05','GPG-CD-KPI-V1',3,12,NULL,'verified_subjective_misstatement'),
('P06','GPG-CD-KPI-V1',15,NULL,'Không đạt','verified_concealment_or_dishonesty'),
('P07','GPG-CD-KPI-V1',10,NULL,'Không đạt','verified_privacy_violation');

-- V2/V3 contain demonstration rows in the production Flyway chain. Preserve those rows for existing
-- screens, but explicitly exclude their stable business keys from KPI source sets.
INSERT INTO kpi_source_exclusions(source_module, source_record_key, reason)
VALUES
('DM_CONG_DOAN','VCS','Flyway V2 demonstration governance profile'),
('DM_CONG_DOAN','GPL','Flyway V2 demonstration governance profile'),
('DM_CONG_DOAN','AZC','Flyway V2 demonstration governance profile'),
('DM_CONG_DOAN','GPD','Flyway V2 demonstration governance profile'),
('DOAN_VIEN','NV3811','Flyway V2 demonstration data'),
('DOAN_VIEN','NV1182','Flyway V2 demonstration data'),
('DOAN_VIEN','NV2201','Flyway V2 demonstration data'),
('DOAN_VIEN','NV3302','Flyway V2 demonstration data'),
('CHAM_SOC_NLD','CS-0826-01','Flyway V2 demonstration data'),
('CHAM_SOC_NLD','CS-0826-02','Flyway V2 demonstration data'),
('CHAM_SOC_NLD','CS-0826-03','Flyway V2 demonstration data'),
('SO_KIEN_NGHI','UV-003','Flyway V2 demonstration data'),
('SO_KIEN_NGHI','UV-006','Flyway V2 demonstration data'),
('SO_KIEN_NGHI','UV-008','Flyway V2 demonstration data'),
('HOAT_DONG','ACT-0826-01','Flyway V2 demonstration data'),
('HOAT_DONG','ACT-0926-01','Flyway V2 demonstration data'),
('HOAT_DONG','ACT-1026-02','Flyway V2 demonstration data'),
('TAI_CHINH_CD','TC-0826-001','Flyway V2 demonstration data'),
('TAI_CHINH_CD','TC-0826-002','Flyway V2 demonstration data'),
('TAI_CHINH_CD','TC-0826-003','Flyway V2 demonstration data'),
('TAI_CHINH_CD','TC-0826-004','Flyway V2 demonstration data'),
('BAO_CAO_DINH_KY','VCS:2026-08','Flyway V2 demonstration data'),
('BAO_CAO_DINH_KY','GPL:2026-08','Flyway V2 demonstration data'),
('KHAO_SAT','KS-0826-VCS','Flyway V3 demonstration data'),
('KHAO_SAT','KS-0826-GPL','Flyway V3 demonstration data');

CREATE INDEX idx_kpi_run_comparison ON kpi_runs(period_type, period_start, period_end, version_id, run_status);
CREATE INDEX idx_kpi_detail_run ON kpi_result_details(run_id);
CREATE INDEX idx_kpi_evidence_result ON kpi_evidence(result_id);
CREATE INDEX idx_kpi_adjustment_period ON kpi_adjustments(union_unit_id, period_start, period_end, approved_at);
