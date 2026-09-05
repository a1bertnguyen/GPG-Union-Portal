-- V3 is additive: previously locked V1/V2 runs remain immutable.
UPDATE kpi_versions SET status = 'SUPERSEDED' WHERE version_id = 'GPG-CD-KPI-V2';
INSERT INTO kpi_versions(version_id,name,effective_from,status,score_scale,round_display,bonus_cap,data_quality_final_threshold,created_by,approved_by,approved_at)
SELECT 'GPG-CD-KPI-V3','KPI CĐCS — lịch sử hoạt động',effective_from,'ACTIVE',score_scale,round_display,bonus_cap,data_quality_final_threshold,created_by,approved_by,approved_at FROM kpi_versions WHERE version_id='GPG-CD-KPI-V2';
INSERT INTO kpi_definitions(version_id,kpi_code,group_code,name,weight,direction,target_value,max_allowed_value,mandatory,na_allowed,source_module,numerator_rule,denominator_rule,evidence_rule)
SELECT 'GPG-CD-KPI-V3',kpi_code,group_code,name,
CASE kpi_code WHEN 'CARE01' THEN 3 WHEN 'CARE02' THEN 5 WHEN 'CARE03' THEN 5 WHEN 'CARE04' THEN 3 ELSE weight END,
direction,target_value,max_allowed_value,mandatory,na_allowed,source_module,numerator_rule,denominator_rule,evidence_rule FROM kpi_definitions WHERE version_id='GPG-CD-KPI-V2';
INSERT INTO kpi_definitions(version_id,kpi_code,group_code,name,weight,direction,target_value,mandatory,na_allowed,source_module,numerator_rule,denominator_rule,evidence_rule)
VALUES ('GPG-CD-KPI-V3','CARE05','CARE','Bao phủ chăm lo trên tổng nghĩa vụ',4,'HIGHER_BETTER',1,TRUE,FALSE,'CHAM_SOC_NLD','unique_birthday_employees_completed_plus_other_cases_completed','approved_year_end_employees_plus_other_cases','population_and_welfare_ids');
INSERT INTO kpi_classification_rules(version_id,minimum_score,label,sort_order) SELECT 'GPG-CD-KPI-V3',minimum_score,label,sort_order FROM kpi_classification_rules WHERE version_id='GPG-CD-KPI-V2';
INSERT INTO kpi_classification_gates(version_id,gate_code,classification_cap,detection_rule) SELECT 'GPG-CD-KPI-V3',gate_code,classification_cap,detection_rule FROM kpi_classification_gates WHERE version_id='GPG-CD-KPI-V2';
INSERT INTO penalty_rules(penalty_code,version_id,points_per_case,period_cap,classification_cap,detection_rule) SELECT penalty_code,'GPG-CD-KPI-V3',points_per_case,period_cap,classification_cap,detection_rule FROM penalty_rules WHERE version_id='GPG-CD-KPI-V2';
INSERT INTO sla_rules(sla_code,version_id,case_type,priority,duration_value,duration_unit,business_calendar_id) SELECT sla_code,'GPG-CD-KPI-V3',case_type,priority,duration_value,duration_unit,business_calendar_id FROM sla_rules WHERE version_id='GPG-CD-KPI-V2';
ALTER TABLE welfare_records ADD COLUMN member_id BIGINT NULL;
ALTER TABLE welfare_records ADD COLUMN cancellation_reason VARCHAR(1000) NULL;
ALTER TABLE union_activities ADD COLUMN cancellation_reason VARCHAR(1000) NULL;
ALTER TABLE kpi_runs ADD COLUMN unit_code_snapshot VARCHAR(100) NULL;
ALTER TABLE kpi_runs ADD COLUMN unit_name_snapshot VARCHAR(500) NULL;
ALTER TABLE kpi_runs ADD COLUMN population_snapshot_id BIGINT NULL;
ALTER TABLE kpi_runs ADD COLUMN active_employee_count BIGINT NULL;
ALTER TABLE kpi_runs ADD COLUMN active_union_member_count BIGINT NULL;
ALTER TABLE kpi_run_warnings ADD COLUMN due_at DATE NULL;
CREATE TABLE kpi_population_snapshots (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, union_unit_id BIGINT NOT NULL, population_year INT NOT NULL,
 revision INT NOT NULL, status VARCHAR(30) NOT NULL, reconciliation_note VARCHAR(2000) NOT NULL,
 prepared_by VARCHAR(150) NOT NULL, submitted_at TIMESTAMP NULL, approved_by VARCHAR(150) NULL,
 approved_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(union_unit_id,population_year,revision), FOREIGN KEY(union_unit_id) REFERENCES union_units(id)
);
CREATE TABLE kpi_population_members (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, snapshot_id BIGINT NOT NULL, member_id BIGINT NOT NULL,
 employee_code VARCHAR(100) NOT NULL, full_name VARCHAR(255) NOT NULL, union_member BOOLEAN NOT NULL,
 profile_complete BOOLEAN NOT NULL, identity_declared BOOLEAN NOT NULL, identity_unique BOOLEAN NOT NULL,
 UNIQUE(snapshot_id,member_id), FOREIGN KEY(snapshot_id) REFERENCES kpi_population_snapshots(id)
);
CREATE TABLE kpi_activity_statistics (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, run_id BIGINT NOT NULL, statistic_payload LONGTEXT NOT NULL,
 UNIQUE(run_id), FOREIGN KEY(run_id) REFERENCES kpi_runs(id)
);

