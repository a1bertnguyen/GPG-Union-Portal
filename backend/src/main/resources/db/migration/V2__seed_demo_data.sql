INSERT INTO union_units (code, name, company_name, location, chairperson, term_start, term_end, decision_number, legal_status, contact_person)
VALUES
('VCS', 'CĐCS VCS', 'VCS', 'VP-TCT', 'Nguyễn Văn Minh', '2025-01-01', '2030-12-31', 'QD-VCS-2025', 'ACTIVE', 'Trần Thu Hà'),
('GPL', 'CĐCS GPL', 'GPL', 'Linh Xuân', 'Lê Hoàng Nam', '2025-01-01', '2030-12-31', 'QD-GPL-2025', 'ACTIVE', 'Phạm Mai Anh'),
('AZC', 'CĐCS AZC', 'AZC', 'Hải Phòng', 'Đỗ Minh Tuấn', '2025-01-01', '2030-12-31', 'QD-AZC-2025', 'ACTIVE', 'Vũ Thanh Hương'),
('GPD', 'CĐCS GPD', 'GPD', 'Hà Nội', 'Hoàng Quốc Việt', '2025-01-01', '2030-12-31', 'QD-GPD-2025', 'ACTIVE', 'Ngô Bích Ngọc');

INSERT INTO members (employee_code, full_name, union_unit_id, job_title, workplace, join_date, membership_status, employment_status, email, phone)
VALUES
('NV3811', 'Nguyễn Trần Hải Yến', 1, 'Chuyên viên', 'VP-TCT', '2023-03-15', 'MEMBER', 'ACTIVE', 'yen.nguyen@gpg.vn', '0901000001'),
('NV1182', 'Nguyễn Văn Quang', 1, 'Quản lý', 'VP-TCT', '2022-06-20', 'MEMBER', 'ACTIVE', 'quang.nguyen@gpg.vn', '0901000002'),
('NV2201', 'Nguyễn A', 2, 'Nhân viên', 'Linh Xuân', NULL, 'NOT_JOINED', 'ACTIVE', 'a.nguyen@gpg.vn', '0901000003'),
('NV3302', 'Trần B', 3, 'Nhân viên', 'Hải Phòng', '2024-04-12', 'MEMBER', 'ACTIVE', 'b.tran@gpg.vn', '0901000004');

INSERT INTO welfare_records (record_code, welfare_type, union_unit_id, beneficiary_name, event_date, status, amount, document_status, notes)
VALUES
('CS-0826-01', 'BIRTHDAY', 2, 'Nguyễn A', '2026-08-13', 'COMPLETED', 500000, 'COMPLETE', 'Quà sinh nhật tháng 8'),
('CS-0826-02', 'VISIT', 1, 'Nguyễn Văn Quang', '2026-08-14', 'PENDING_APPROVAL', 1000000, 'COMPLETE', 'Thăm hỏi đoàn viên'),
('CS-0826-03', 'FUNERAL', 3, 'Trần B', '2026-08-11', 'COMPLETED', 2000000, 'INCOMPLETE', 'Còn thiếu ảnh xác nhận');

INSERT INTO labor_cases (case_code, received_date, union_unit_id, issue_group, severity, owner_name, deadline, status, description, affected_people, result_text, overdue_reason)
VALUES
('UV-003', '2026-08-10', 3, 'Ca làm việc', 'HIGH', 'HR + CĐ GPG', '2026-08-15', 'IN_PROGRESS', 'Kiến nghị liên quan lịch ca làm việc', 12, 'Đang tổng hợp dữ liệu', 'Đang chờ dữ liệu từ đơn vị'),
('UV-006', '2026-08-11', 2, 'Phúc lợi', 'MEDIUM', 'CĐCS', '2026-08-26', 'WAITING_RESPONSE', 'Đề nghị làm rõ chính sách phúc lợi', 3, NULL, NULL),
('UV-008', '2026-08-12', 1, 'Điều kiện làm việc', 'HIGH', 'BNC + CĐCS', '2026-08-14', 'IN_PROGRESS', 'Phản ánh điều kiện làm việc tại khu vực sản xuất', 18, 'Đã xác minh hiện trạng', 'Chờ phương án khắc phục');

INSERT INTO union_activities (activity_code, name, union_unit_id, event_date, status, objective, planned_budget, actual_cost, participant_count, usefulness_score, report_completed, follow_up_owner, follow_up_deadline)
VALUES
('ACT-0826-01', 'Bữa trưa kết nối', 2, '2026-08-25', 'COMPLETED', 'Tăng kết nối nội bộ', 4000000, 3200000, 32, 4.60, TRUE, NULL, NULL),
('ACT-0926-01', 'Lắng nghe tuyến đầu', 1, '2026-09-10', 'PLANNED', 'Thu thập ý kiến NLĐ tuyến đầu', 6000000, 0, 0, NULL, FALSE, 'CĐCS VCS', '2026-09-20'),
('ACT-1026-02', 'Skill-sharing', 4, '2026-10-18', 'PLANNED', 'Chia sẻ kỹ năng giữa các đơn vị', 5000000, 0, 0, NULL, FALSE, 'CĐCS GPD', '2026-10-25');

INSERT INTO finance_entries (entry_code, union_unit_id, transaction_date, entry_type, category, amount, description, document_number, document_status)
VALUES
('TC-0826-001', 1, '2026-08-05', 'INCOME', 'Đoàn phí', 12000000, 'Thu đoàn phí tháng 8', 'PT-001', 'COMPLETE'),
('TC-0826-002', 2, '2026-08-25', 'EXPENSE', 'Hoạt động', 3200000, 'Chi chương trình Bữa trưa kết nối', 'PC-001', 'COMPLETE'),
('TC-0826-003', 3, '2026-08-11', 'EXPENSE', 'Chăm lo', 2000000, 'Chi hỗ trợ hiếu', 'PC-002', 'INCOMPLETE'),
('TC-0826-004', 1, '2026-08-14', 'EXPENSE', 'Chăm lo', 1000000, 'Chi thăm hỏi đoàn viên', 'PC-003', 'COMPLETE');

INSERT INTO monthly_reports (union_unit_id, report_month, prepared_by, plan_next_month, support_request, status, submitted_at)
VALUES
(1, '2026-08-01', 'Trần Thu Hà', 'Tổ chức chương trình lắng nghe tuyến đầu', 'Hỗ trợ truyền thông nội bộ', 'SUBMITTED', CURRENT_TIMESTAMP),
(2, '2026-08-01', 'Phạm Mai Anh', 'Hoàn thiện kế hoạch tháng 9', 'Không', 'DRAFT', NULL);
