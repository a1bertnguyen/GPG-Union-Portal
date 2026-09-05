-- Bộ chỉ tiêu KPI V2: chỉ gồm các KPI mà schema hiện tại tính được tử số VÀ mẫu số từ dữ liệu thật.
-- V1 (31 mã) để lại nguyên vẹn cho mục đích lưu vết nhưng chuyển sang SUPERSEDED — trạng thái này không
-- nằm trong SELECTABLE_VERSION_STATUSES của GpgKpiEngine nên V1 không còn được chọn để tính, cũng không
-- xuất hiện trong GET /api/kpi/metadata. Chưa có bản chốt kỳ nào được lưu theo V1 nên không mất lịch sử.
-- Chỉ dùng ALTER/INSERT/UPDATE thuần để chuỗi migration còn chạy được trên H2 (MODE=MySQL) trong test.

UPDATE kpi_versions SET status = 'SUPERSEDED' WHERE version_id = 'GPG-CD-KPI-V1';

-- Mốc hoàn tất thật của hồ sơ chăm lo. Trước đây CARE02 phải lấy updated_at làm đại diện, nhưng bất kỳ
-- lần sửa nào sau khi hoàn thành cũng làm sai mốc đó.
ALTER TABLE welfare_records ADD COLUMN completed_at TIMESTAMP NULL;
UPDATE welfare_records SET completed_at = updated_at WHERE status = 'COMPLETED';

INSERT INTO kpi_versions(version_id, name, effective_from, status, score_scale, round_display,
                         bonus_cap, data_quality_final_threshold, created_by, approved_by, approved_at)
VALUES ('GPG-CD-KPI-V2', 'KPI Công đoàn cơ sở GPG V2', '2026-01-01', 'ACTIVE', 100, 2,
        3, 0.80, 'system', 'system', CURRENT_TIMESTAMP);

INSERT INTO kpi_classification_rules(version_id, minimum_score, label, sort_order)
VALUES
('GPG-CD-KPI-V2',90,'Xuất sắc',1),
('GPG-CD-KPI-V2',80,'Tốt',2),
('GPG-CD-KPI-V2',65,'Khá',3),
('GPG-CD-KPI-V2',50,'Trung bình',4),
('GPG-CD-KPI-V2',0,'Không đạt',5);

INSERT INTO kpi_classification_gates(version_id, gate_code, classification_cap, detection_rule)
VALUES
('GPG-CD-KPI-V2','INTEGRITY_VIOLATION','Không đạt','approved_penalty_P06_or_P07'),
('GPG-CD-KPI-V2','SERIOUS_OPEN_CASE','Trung bình','critical_grievance_or_overdue_care_open'),
('GPG-CD-KPI-V2','MISSING_MANDATORY_REPORT','Khá','mandatory_report_not_submitted'),
('GPG-CD-KPI-V2','GOVERNANCE_INCOMPLETE','Khá','required_legal_or_bch_profile_incomplete');

INSERT INTO penalty_rules(penalty_code, version_id, points_per_case, period_cap, classification_cap, detection_rule)
VALUES
('P01','GPG-CD-KPI-V2',5,15,'Khá','mandatory_report_not_submitted'),
('P02','GPG-CD-KPI-V2',2,10,'Trung bình','seriously_overdue_case'),
('P03','GPG-CD-KPI-V2',5,15,NULL,'wrong_care_target_or_policy'),
('P04','GPG-CD-KPI-V2',3,12,NULL,'finance_document_missing_after_due'),
('P05','GPG-CD-KPI-V2',3,12,NULL,'verified_subjective_misstatement'),
('P06','GPG-CD-KPI-V2',15,NULL,'Không đạt','verified_concealment_or_dishonesty'),
('P07','GPG-CD-KPI-V2',10,NULL,'Không đạt','verified_privacy_violation');

-- REPORT_SUBMISSION là mức SLA mà V21 cố tình bỏ trống chờ phê duyệt. Không có nó thì REP01 mãi thiếu dữ
-- liệu và P01 không bao giờ phạt được. Mức đã chốt: 5 ngày làm việc sau khi kết thúc tháng báo cáo.
INSERT INTO sla_rules(sla_code, version_id, case_type, priority, duration_value, duration_unit, business_calendar_id)
VALUES
('REPORT_SUBMISSION','GPG-CD-KPI-V2','REPORT','ALL',5,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_ACK','GPG-CD-KPI-V2','GRIEVANCE','ALL',1,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_ASSIGN','GPG-CD-KPI-V2','GRIEVANCE','ALL',2,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_NORMAL','GPG-CD-KPI-V2','GRIEVANCE','NORMAL',7,'BUSINESS_DAY','GPG_DEFAULT'),
('GRV_COMPLEX','GPG-CD-KPI-V2','GRIEVANCE','COMPLEX',15,'BUSINESS_DAY','GPG_DEFAULT'),
('CARE_URGENT','GPG-CD-KPI-V2','CARE','URGENT',1,'BUSINESS_DAY','GPG_DEFAULT'),
('CARE_NORMAL','GPG-CD-KPI-V2','CARE','NORMAL',3,'BUSINESS_DAY','GPG_DEFAULT'),
('ACT_POST_REPORT','GPG-CD-KPI-V2','ACTIVITY','ALL',5,'BUSINESS_DAY','GPG_DEFAULT'),
('MEMBER_CHANGE','GPG-CD-KPI-V2','MEMBER_CHANGE','ALL',5,'BUSINESS_DAY','GPG_DEFAULT');

-- Lịch GPG_DEFAULT: reportDeadline/slaDeadline mặc định coi thứ Bảy và Chủ nhật là ngày nghỉ, nên chỉ cần
-- khai các ngày lễ. Ngày lễ dương lịch cố định là chắc chắn; Tết Nguyên đán 2026 (mồng 1 là 17/02/2026)
-- khai theo 5 ngày nghỉ luật định.
-- ADMIN PHẢI BỔ SUNG HÀNG NĂM: Giỗ Tổ Hùng Vương (10/3 âm lịch), ngày nghỉ thứ hai của Quốc khánh và các
-- ngày nghỉ bù/hoán đổi theo thông báo của Chính phủ. Thiếu dòng nào thì ngày đó bị tính là ngày làm việc
-- và hạn SLA sẽ sớm hơn thực tế.
INSERT INTO business_calendar_days(business_calendar_id, calendar_date, working_day, description)
VALUES
('GPG_DEFAULT','2026-01-01',FALSE,'Tết Dương lịch'),
('GPG_DEFAULT','2026-02-16',FALSE,'Tết Nguyên đán'),
('GPG_DEFAULT','2026-02-17',FALSE,'Tết Nguyên đán - mồng 1'),
('GPG_DEFAULT','2026-02-18',FALSE,'Tết Nguyên đán - mồng 2'),
('GPG_DEFAULT','2026-02-19',FALSE,'Tết Nguyên đán - mồng 3'),
('GPG_DEFAULT','2026-02-20',FALSE,'Tết Nguyên đán - mồng 4'),
('GPG_DEFAULT','2026-04-30',FALSE,'Ngày Giải phóng miền Nam'),
('GPG_DEFAULT','2026-05-01',FALSE,'Ngày Quốc tế Lao động'),
('GPG_DEFAULT','2026-09-02',FALSE,'Quốc khánh'),
('GPG_DEFAULT','2027-01-01',FALSE,'Tết Dương lịch'),
('GPG_DEFAULT','2027-04-30',FALSE,'Ngày Giải phóng miền Nam'),
('GPG_DEFAULT','2027-05-01',FALSE,'Ngày Quốc tế Lao động'),
('GPG_DEFAULT','2027-09-02',FALSE,'Quốc khánh');

-- 23 chỉ tiêu, tổng trọng số 100: GOV 10 · DATA 18 · REP 14 · CARE 20 · GRV 16 · ACT 12 · FIN 10.
-- Mỗi mã ở đây đều có calculator trong GpgKpiEngine.metric() đọc được cả tử số và mẫu số từ dữ liệu nghiệp
-- vụ đang có. Các chỉ tiêu của V1 cần nguồn chưa tồn tại (tập huấn, sổ thay đổi tổ chức, PHEDUYET_LOG, phản
-- hồi NLĐ, mục tiêu chương trình đã duyệt, đối soát số dư) không đưa vào V2 — chúng chỉ tạo MISSING_DATA và
-- kéo điểm mọi đơn vị về 0. Khi có nguồn thì mở V3, không sửa V2.
INSERT INTO kpi_definitions
    (version_id, kpi_code, group_code, name, weight, direction, target_value, max_allowed_value,
     mandatory, na_allowed, source_module, numerator_rule, denominator_rule, evidence_rule)
VALUES
('GPG-CD-KPI-V2','GOV01','GOV','Hồ sơ pháp lý CĐCS hợp lệ',5,'BOOLEAN',1,NULL,TRUE,FALSE,'DM_CONG_DOAN','decision_number_and_active_term_covering_cutoff','one_union_profile','union_unit_id'),
('GPG-CD-KPI-V2','GOV02','GOV','BCH và đầu mối liên hệ đầy đủ',5,'BOOLEAN',1,NULL,TRUE,FALSE,'DM_CONG_DOAN','chairperson_and_contact_person_present','one_union_profile','union_unit_id'),
('GPG-CD-KPI-V2','DATA01','DATA','Hồ sơ đoàn viên đủ trường cốt lõi',6,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'DOAN_VIEN','members_with_required_profile_fields','active_union_members','membership_ids'),
('GPG-CD-KPI-V2','DATA02','DATA','Biến động đoàn viên ghi nhận trong SLA',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'DOAN_VIEN','changes_recorded_within_member_change_sla','member_changes_in_period','member_change_ids'),
('GPG-CD-KPI-V2','DATA03','DATA','Không trùng CCCD/số điện thoại',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'DOAN_VIEN','members_without_duplicate_identity','members_with_identity_declared','membership_ids'),
('GPG-CD-KPI-V2','DATA04','DATA','Tỷ lệ tham gia công đoàn',5,'HIGHER_BETTER',0.90,NULL,TRUE,FALSE,'DOAN_VIEN','members_with_membership_status_member','active_employees_in_unit','membership_ids'),
('GPG-CD-KPI-V2','REP01','REP','Báo cáo định kỳ nộp đúng hạn',9,'HIGHER_BETTER',1,NULL,TRUE,FALSE,'BAO_CAO_DINH_KY','reports_submitted_within_report_sla','report_months_due_in_period','report_ids'),
('GPG-CD-KPI-V2','REP02','REP','Báo cáo có kế hoạch kỳ sau và đề xuất',5,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'BAO_CAO_DINH_KY','reports_with_plan_and_support_request','submitted_or_approved_reports','report_ids'),
('GPG-CD-KPI-V2','CARE01','CARE','Ghi nhận hồ sơ chăm lo kịp thời',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','records_created_within_care_sla','care_cases_occurred_in_period','care_case_ids'),
('GPG-CD-KPI-V2','CARE02','CARE','Hoàn tất chăm lo trước hạn',6,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','records_completed_at_or_before_deadline','care_cases_due_in_period','care_case_ids'),
('GPG-CD-KPI-V2','CARE03','CARE','Hồ sơ chăm lo khép kín',6,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','completed_with_policy_documents_receipt_and_file','completed_care_cases','care_and_document_ids'),
('GPG-CD-KPI-V2','CARE04','CARE','Chi đúng định mức chính sách',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'CHAM_SOC_NLD','amount_equals_policy_standard_amount','care_cases_with_policy','care_policy_ids'),
('GPG-CD-KPI-V2','GRV01','GRV','Ghi sổ kiến nghị trong SLA tiếp nhận',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','cases_recorded_within_ack_sla','grievances_received_in_period','grievance_ids'),
('GPG-CD-KPI-V2','GRV02','GRV','Xử lý kiến nghị đúng SLA',6,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','closed_at_or_before_deadline','cases_due_in_period','grievance_ids'),
('GPG-CD-KPI-V2','GRV03','GRV','Đóng kiến nghị đúng quy trình',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'SO_KIEN_NGHI','closed_with_result_response_and_approval','closed_cases_in_period','grievance_and_document_ids'),
('GPG-CD-KPI-V2','GRV04','GRV','Tỷ lệ giải quyết trên phát sinh',3,'HIGHER_BETTER',0.90,NULL,FALSE,TRUE,'SO_KIEN_NGHI','cases_closed_in_period','grievances_received_in_period','grievance_ids'),
('GPG-CD-KPI-V2','ACT01','ACT','Hoàn thành chương trình theo kế hoạch',4,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','completed_activities','approved_or_running_activities','activity_ids'),
('GPG-CD-KPI-V2','ACT02','ACT','Tỷ lệ tham gia trên số mời',3,'HIGHER_BETTER',0.80,NULL,FALSE,TRUE,'HOAT_DONG','actual_participants','invited_participants','activity_ids'),
('GPG-CD-KPI-V2','ACT03','ACT','Báo cáo sau chương trình đủ hồ sơ',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','completed_with_report_documents_and_media','completed_activities','activity_and_media_ids'),
('GPG-CD-KPI-V2','ACT04','ACT','Điểm hữu ích chương trình',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','average_usefulness_score','usefulness_scale_max','activity_ids'),
('GPG-CD-KPI-V2','FIN01','FIN','Giao dịch có chứng từ hợp lệ',5,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'TAI_CHINH_CD','entries_with_complete_documents_and_file','entries_in_period','transaction_and_document_ids'),
('GPG-CD-KPI-V2','FIN02','FIN','Chi chăm lo khớp hồ sơ',3,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'TAI_CHINH_CD','approved_care_with_matching_finance_entry','approved_care_cases','care_and_transaction_ids'),
('GPG-CD-KPI-V2','FIN03','FIN','Tuân thủ ngân sách chương trình',2,'HIGHER_BETTER',1,NULL,FALSE,TRUE,'HOAT_DONG','activities_within_planned_budget','budget_controlled_activities','activity_ids');
