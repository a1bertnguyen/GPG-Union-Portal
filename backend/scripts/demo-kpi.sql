-- Dữ liệu minh họa KPI CĐCS (chỉ dùng local/demo).
-- Chạy sau khi Flyway đã tạo đủ bảng. Tất cả mã đều bắt đầu bằng DEMO-KPI.
-- Kỳ xem trên màn KPI: Năm 2025.

START TRANSACTION;

SET @demo_code = 'DEMO-KPI-2025';

-- Cho phép chạy lại script: chỉ xóa đúng CĐCS demo này.
SET @demo_unit_id = (SELECT id FROM union_units WHERE code = @demo_code LIMIT 1);
DELETE FROM kpi_population_members WHERE snapshot_id IN (SELECT id FROM kpi_population_snapshots WHERE union_unit_id = @demo_unit_id);
DELETE FROM kpi_population_snapshots WHERE union_unit_id = @demo_unit_id;
DELETE FROM member_changes WHERE member_id IN (SELECT id FROM members WHERE union_unit_id = @demo_unit_id);
DELETE FROM welfare_records WHERE union_unit_id = @demo_unit_id;
DELETE FROM labor_cases WHERE union_unit_id = @demo_unit_id;
DELETE FROM union_activities WHERE union_unit_id = @demo_unit_id;
DELETE FROM finance_entries WHERE union_unit_id = @demo_unit_id;
DELETE FROM monthly_reports WHERE union_unit_id = @demo_unit_id;
DELETE FROM members WHERE union_unit_id = @demo_unit_id;
DELETE FROM union_units WHERE id = @demo_unit_id;

INSERT INTO union_units (code, name, company_name, location, chairperson, term_start, term_end,
                         decision_number, legal_status, contact_person)
VALUES (@demo_code, 'CĐCS DEMO KPI', 'Công ty Demo KPI', 'Văn phòng demo',
        'Nguyễn Minh Demo', '2020-01-01', '2030-12-31', 'QD-DEMO-KPI-2025', 'ACTIVE',
        'demo@gpg.vn');
SET @demo_unit_id = LAST_INSERT_ID();

-- 10 nhân sự hoạt động, hồ sơ đủ trường lõi.
INSERT INTO members (employee_code, full_name, union_unit_id, job_title, workplace, join_date,
                     membership_status, employment_status, email, phone, company,
                     national_id, gender, start_work_date, current_residence)
VALUES
('DEMO-KPI-NV01','Nguyễn An',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-01-10','MEMBER','ACTIVE','demo01@gpg.vn','0905000001','Công ty Demo KPI','DEMO-ID-01','MALE','2020-01-10','TP.HCM'),
('DEMO-KPI-NV02','Trần Bình',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-02-10','MEMBER','ACTIVE','demo02@gpg.vn','0905000002','Công ty Demo KPI','DEMO-ID-02','MALE','2020-02-10','TP.HCM'),
('DEMO-KPI-NV03','Lê Chi',@demo_unit_id,'Chuyên viên','Văn phòng demo','2020-03-10','MEMBER','ACTIVE','demo03@gpg.vn','0905000003','Công ty Demo KPI','DEMO-ID-03','FEMALE','2020-03-10','TP.HCM'),
('DEMO-KPI-NV04','Phạm Dũng',@demo_unit_id,'Chuyên viên','Văn phòng demo','2020-04-10','MEMBER','ACTIVE','demo04@gpg.vn','0905000004','Công ty Demo KPI','DEMO-ID-04','MALE','2020-04-10','TP.HCM'),
('DEMO-KPI-NV05','Võ Hà',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-05-10','MEMBER','ACTIVE','demo05@gpg.vn','0905000005','Công ty Demo KPI','DEMO-ID-05','FEMALE','2020-05-10','TP.HCM'),
('DEMO-KPI-NV06','Đỗ Khánh',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-06-10','MEMBER','ACTIVE','demo06@gpg.vn','0905000006','Công ty Demo KPI','DEMO-ID-06','MALE','2020-06-10','TP.HCM'),
('DEMO-KPI-NV07','Bùi Lan',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-07-10','MEMBER','ACTIVE','demo07@gpg.vn','0905000007','Công ty Demo KPI','DEMO-ID-07','FEMALE','2020-07-10','TP.HCM'),
('DEMO-KPI-NV08','Hoàng Mai',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-08-10','MEMBER','ACTIVE','demo08@gpg.vn','0905000008','Công ty Demo KPI','DEMO-ID-08','FEMALE','2020-08-10','TP.HCM'),
('DEMO-KPI-NV09','Ngô Nam',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-09-10','MEMBER','ACTIVE','demo09@gpg.vn','0905000009','Công ty Demo KPI','DEMO-ID-09','MALE','2020-09-10','TP.HCM'),
('DEMO-KPI-NV10','Đặng Oanh',@demo_unit_id,'Nhân viên','Văn phòng demo','2020-10-10','MEMBER','ACTIVE','demo10@gpg.vn','0905000010','Công ty Demo KPI','DEMO-ID-10','FEMALE','2020-10-10','TP.HCM');

-- Snapshot đã đối soát/phê duyệt để CARE05 có mẫu số = 10.
INSERT INTO kpi_population_snapshots (union_unit_id,population_year,revision,status,reconciliation_note,
                                      prepared_by,submitted_at,approved_by,approved_at)
VALUES (@demo_unit_id,2025,1,'APPROVED','Dữ liệu minh họa: 10 nhân sự cuối năm','DEMO',
        '2025-12-31 09:00:00','DEMO-ADMIN','2026-01-02 09:00:00');
SET @demo_snapshot_id = LAST_INSERT_ID();
INSERT INTO kpi_population_members (snapshot_id,member_id,employee_code,full_name,union_member,
                                    profile_complete,identity_declared,identity_unique)
SELECT @demo_snapshot_id,id,employee_code,full_name,TRUE,TRUE,TRUE,TRUE
FROM members WHERE union_unit_id=@demo_unit_id ORDER BY id;

-- 8/10 nhân sự đã nhận chăm lo sinh nhật = 80%; 2 hồ sơ còn PENDING để nhìn thấy chênh lệch.
INSERT INTO welfare_records (record_code,welfare_type,union_unit_id,beneficiary_name,member_id,event_date,
                             status,amount,document_status,completed_at,policy_name,standard_amount,receipt_status,has_image,notes)
SELECT CONCAT('DEMO-KPI-BDAY-',LPAD(n,3,'0')),'BIRTHDAY',@demo_unit_id,m.full_name,m.id,
       '2025-06-15','COMPLETED',300000,'COMPLETE','2025-06-20 10:00:00','Quà sinh nhật demo',300000,'COMPLETE',TRUE,'Đã hoàn thành'
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) x
JOIN members m ON m.union_unit_id=@demo_unit_id AND m.employee_code=CONCAT('DEMO-KPI-NV',LPAD(x.n,2,'0'));
INSERT INTO welfare_records (record_code,welfare_type,union_unit_id,beneficiary_name,member_id,event_date,status,amount,document_status,policy_name,standard_amount,receipt_status,notes)
SELECT CONCAT('DEMO-KPI-BDAY-',LPAD(n,3,'0')),'BIRTHDAY',@demo_unit_id,m.full_name,m.id,'2025-06-15','PENDING_APPROVAL',300000,'INCOMPLETE','Quà sinh nhật demo',300000,'INCOMPLETE','Chưa hoàn tất'
FROM (SELECT 9 n UNION ALL SELECT 10) x
JOIN members m ON m.union_unit_id=@demo_unit_id AND m.employee_code=CONCAT('DEMO-KPI-NV',LPAD(x.n,2,'0'));

-- Kiến nghị: 4/5 đã đóng; hoạt động: 4/5 hoàn thành; tài chính: 5/6 đủ chứng từ.
INSERT INTO labor_cases (case_code,received_date,union_unit_id,issue_group,severity,owner_name,deadline,status,description,affected_people,result_text,approved_by,approved_at,response_date,requester_name)
VALUES
('DEMO-KPI-GRV01','2025-02-05',@demo_unit_id,'Phúc lợi','MEDIUM','CĐCS Demo','2025-02-20','CLOSED','Đề nghị bổ sung phúc lợi',2,'Đã xử lý','DEMO-ADMIN','2025-02-15','2025-02-14','Đoàn viên demo'),
('DEMO-KPI-GRV02','2025-04-05',@demo_unit_id,'Điều kiện làm việc','HIGH','CĐCS Demo','2025-04-20','CLOSED','Phản ánh điều kiện làm việc',4,'Đã xử lý','DEMO-ADMIN','2025-04-18','2025-04-17','Đoàn viên demo'),
('DEMO-KPI-GRV03','2025-06-05',@demo_unit_id,'Tiền lương','MEDIUM','CĐCS Demo','2025-06-20','CLOSED','Đề nghị giải thích lương',3,'Đã xử lý','DEMO-ADMIN','2025-06-18','2025-06-17','Đoàn viên demo'),
('DEMO-KPI-GRV04','2025-08-05',@demo_unit_id,'An toàn','HIGH','CĐCS Demo','2025-08-20','CLOSED','Đề nghị kiểm tra an toàn',5,'Đã xử lý','DEMO-ADMIN','2025-08-19','2025-08-18','Đoàn viên demo'),
('DEMO-KPI-GRV05','2025-10-05',@demo_unit_id,'Phúc lợi','LOW','CĐCS Demo','2025-10-20','IN_PROGRESS','Đề nghị đang xử lý',1,NULL,NULL,NULL,NULL,'Đoàn viên demo');

INSERT INTO union_activities (activity_code,name,union_unit_id,event_date,status,objective,planned_budget,actual_cost,participant_count,invited_count,check_in_count,workers_reached,usefulness_score,report_completed,document_status,actual_content)
VALUES
('DEMO-KPI-ACT01','Tập huấn 1',@demo_unit_id,'2025-03-10','COMPLETED','Tập huấn',1000000,900000,8,10,8,8,4.5,TRUE,'COMPLETE','Đã thực hiện'),
('DEMO-KPI-ACT02','Tập huấn 2',@demo_unit_id,'2025-05-10','COMPLETED','Tập huấn',1000000,950000,9,10,9,9,4.2,TRUE,'COMPLETE','Đã thực hiện'),
('DEMO-KPI-ACT03','Đối thoại 1',@demo_unit_id,'2025-07-10','COMPLETED','Đối thoại',1000000,1000000,10,10,10,10,4.8,TRUE,'COMPLETE','Đã thực hiện'),
('DEMO-KPI-ACT04','Đối thoại 2',@demo_unit_id,'2025-09-10','COMPLETED','Đối thoại',1000000,980000,7,10,7,7,4.0,TRUE,'COMPLETE','Đã thực hiện'),
('DEMO-KPI-ACT05','Kế hoạch cuối năm',@demo_unit_id,'2025-11-10','PLANNED','Kế hoạch',1000000,0,0,10,0,0,NULL,FALSE,'INCOMPLETE',NULL);

INSERT INTO finance_entries (entry_code,union_unit_id,transaction_date,entry_type,category,amount,description,document_number,document_status)
VALUES
('DEMO-KPI-FIN01',@demo_unit_id,'2025-02-01','INCOME','Đoàn phí',1000000,'Thu demo','DEMO-PT-01','COMPLETE'),
('DEMO-KPI-FIN02',@demo_unit_id,'2025-04-01','EXPENSE','Hoạt động',900000,'Chi demo','DEMO-PC-02','COMPLETE'),
('DEMO-KPI-FIN03',@demo_unit_id,'2025-06-01','EXPENSE','Hoạt động',950000,'Chi demo','DEMO-PC-03','COMPLETE'),
('DEMO-KPI-FIN04',@demo_unit_id,'2025-08-01','EXPENSE','Hoạt động',1000000,'Chi demo','DEMO-PC-04','COMPLETE'),
('DEMO-KPI-FIN05',@demo_unit_id,'2025-10-01','EXPENSE','Hoạt động',980000,'Chi demo','DEMO-PC-05','COMPLETE'),
('DEMO-KPI-FIN06',@demo_unit_id,'2025-12-01','EXPENSE','Khác',100000,'Chi demo','DEMO-PC-06','INCOMPLETE');

INSERT INTO monthly_reports (union_unit_id,report_month,prepared_by,plan_next_month,support_request,status,submitted_at)
SELECT @demo_unit_id,DATE(CONCAT('2025-',LPAD(n,2,'0'),'-01')),'DEMO','Kế hoạch tháng sau','Không',
       CASE WHEN n<=10 THEN 'APPROVED' ELSE 'DRAFT' END,
       CASE WHEN n<=10 THEN TIMESTAMP(CONCAT('2025-',LPAD(n,2,'0'),'-05 09:00:00')) ELSE NULL END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12) months;

COMMIT;

SELECT @demo_unit_id AS demo_union_unit_id, @demo_snapshot_id AS demo_population_snapshot_id;
