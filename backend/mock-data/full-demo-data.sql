-- Bộ dữ liệu mock toàn hệ thống, bám theo 17 chính sách chăm lo tại V12.
-- Mục tiêu: mỗi phân hệ đều có dữ liệu theo nhiều CĐCS, quý, năm và trạng thái.

START TRANSACTION;

UPDATE members
SET company = 'VCS', proposed_union_title = 'Ủy viên BCH', professional_title = 'Chuyên viên nhân sự',
    gender = 'FEMALE', ethnicity = 'Kinh', place_of_birth = 'TP. Hồ Chí Minh',
    national_id = '079190000001', party_member = FALSE, education = 'Đại học',
    specialization = 'Quản trị nhân lực', political_theory = 'Sơ cấp', foreign_language = 'Tiếng Anh B1',
    start_work_date = '2021-03-15', current_residence = 'TP. Thủ Đức, TP. Hồ Chí Minh'
WHERE employee_code = 'NV3811';

UPDATE members
SET company = 'VCS', proposed_union_title = 'Tổ trưởng công đoàn', professional_title = 'Quản lý vận hành',
    gender = 'MALE', ethnicity = 'Kinh', place_of_birth = 'Đồng Nai',
    national_id = '075188000002', party_member = TRUE, education = 'Đại học',
    specialization = 'Quản trị kinh doanh', political_theory = 'Trung cấp', foreign_language = 'Tiếng Anh B1',
    start_work_date = '2018-06-20', current_residence = 'Biên Hòa, Đồng Nai'
WHERE employee_code = 'NV1182';

UPDATE members
SET company = 'GPL', professional_title = 'Nhân viên kho', gender = 'MALE', ethnicity = 'Kinh',
    place_of_birth = 'Bình Dương', national_id = '074199000003', party_member = FALSE,
    education = 'Trung cấp', specialization = 'Logistics', foreign_language = 'Cơ bản',
    start_work_date = '2026-05-12', current_residence = 'Dĩ An, Bình Dương'
WHERE employee_code = 'NV2201';

UPDATE members
SET company = 'AZC', proposed_union_title = 'Đoàn viên', professional_title = 'Nhân viên sản xuất',
    gender = 'FEMALE', ethnicity = 'Kinh', place_of_birth = 'Hải Phòng',
    national_id = '031196000004', party_member = FALSE, education = 'Cao đẳng',
    specialization = 'Công nghệ sản xuất', political_theory = 'Sơ cấp', foreign_language = 'Tiếng Anh A2',
    start_work_date = '2022-04-12', current_residence = 'Lê Chân, Hải Phòng'
WHERE employee_code = 'NV3302';

INSERT INTO members
    (employee_code, full_name, union_unit_id, job_title, workplace, join_date, membership_status,
     employment_status, email, phone, company, proposed_union_title, professional_title, gender,
     ethnicity, place_of_birth, national_id, party_member, education, specialization,
     political_theory, foreign_language, start_work_date, current_residence)
VALUES
    ('DEMO-VCS-01', 'Trần Ngọc Mai', (SELECT id FROM union_units WHERE code = 'VCS'), 'Chuyên viên kế toán', 'VP-TCT', '2020-02-10', 'MEMBER', 'ACTIVE', 'mai.tran.demo@gpg.vn', '0911001001', 'VCS', 'Ủy viên BCH', 'Kế toán viên', 'FEMALE', 'Kinh', 'Hà Nội', '001190100001', TRUE, 'Đại học', 'Tài chính kế toán', 'Trung cấp', 'Tiếng Anh B2', '2019-08-01', 'Quận 7, TP. Hồ Chí Minh'),
    ('DEMO-VCS-02', 'Phạm Quốc Huy', (SELECT id FROM union_units WHERE code = 'VCS'), 'Kỹ sư hệ thống', 'VP-TCT', '2023-06-01', 'MEMBER', 'ACTIVE', 'huy.pham.demo@gpg.vn', '0911001002', 'VCS', 'Đoàn viên', 'Kỹ sư CNTT', 'MALE', 'Kinh', 'Đà Nẵng', '048192100002', FALSE, 'Đại học', 'Công nghệ thông tin', 'Sơ cấp', 'Tiếng Anh B2', '2022-11-15', 'Quận Bình Thạnh, TP. Hồ Chí Minh'),
    ('DEMO-VCS-03', 'Lê Thảo Vy', (SELECT id FROM union_units WHERE code = 'VCS'), 'Nhân viên hành chính', 'VP-TCT', '2026-05-15', 'MEMBER', 'ACTIVE', 'vy.le.demo@gpg.vn', '0911001003', 'VCS', 'Đoàn viên', 'Hành chính viên', 'FEMALE', 'Kinh', 'Tiền Giang', '082200100003', FALSE, 'Cao đẳng', 'Hành chính văn phòng', NULL, 'Tiếng Anh A2', '2026-02-12', 'Quận 12, TP. Hồ Chí Minh'),
    ('DEMO-VCS-04', 'Đặng Minh Tâm', (SELECT id FROM union_units WHERE code = 'VCS'), 'Chuyên viên pháp chế', 'VP-TCT', '2021-09-20', 'MEMBER', 'ACTIVE', 'tam.dang.demo@gpg.vn', '0911001004', 'VCS', 'Tổ trưởng công đoàn', 'Chuyên viên pháp chế', 'MALE', 'Kinh', 'Bến Tre', '083189100004', TRUE, 'Sau đại học', 'Luật lao động', 'Cao cấp', 'Tiếng Anh C1', '2017-04-03', 'TP. Thủ Đức, TP. Hồ Chí Minh'),
    ('DEMO-GPL-01', 'Võ Thu Trang', (SELECT id FROM union_units WHERE code = 'GPL'), 'Điều phối vận tải', 'Linh Xuân', '2021-01-18', 'MEMBER', 'ACTIVE', 'trang.vo.demo@gpg.vn', '0912001001', 'GPL', 'Ủy viên BCH', 'Điều phối viên', 'FEMALE', 'Kinh', 'Bình Định', '052190200001', FALSE, 'Đại học', 'Logistics', 'Sơ cấp', 'Tiếng Anh B1', '2020-06-10', 'TP. Thủ Đức, TP. Hồ Chí Minh'),
    ('DEMO-GPL-02', 'Nguyễn Đức Long', (SELECT id FROM union_units WHERE code = 'GPL'), 'Nhân viên kho', 'Linh Xuân', '2024-03-12', 'MEMBER', 'ACTIVE', 'long.nguyen.demo@gpg.vn', '0912001002', 'GPL', 'Đoàn viên', 'Thủ kho', 'MALE', 'Kinh', 'Nghệ An', '040191200002', FALSE, 'Cao đẳng', 'Quản lý kho', NULL, 'Cơ bản', '2023-10-02', 'Dĩ An, Bình Dương'),
    ('DEMO-GPL-03', 'Bùi Khánh Linh', (SELECT id FROM union_units WHERE code = 'GPL'), 'Nhân viên chứng từ', 'Linh Xuân', '2026-04-05', 'MEMBER', 'ACTIVE', 'linh.bui.demo@gpg.vn', '0912001003', 'GPL', 'Đoàn viên', 'Nhân viên chứng từ', 'FEMALE', 'Kinh', 'Nam Định', '036200200003', FALSE, 'Đại học', 'Kinh tế vận tải', NULL, 'Tiếng Anh B1', '2026-01-08', 'Thuận An, Bình Dương'),
    ('DEMO-GPL-04', 'Trương Thành Đạt', (SELECT id FROM union_units WHERE code = 'GPL'), 'Tổ trưởng vận hành', 'Linh Xuân', '2019-07-22', 'MEMBER', 'ACTIVE', 'dat.truong.demo@gpg.vn', '0912001004', 'GPL', 'Tổ trưởng công đoàn', 'Quản lý vận hành', 'MALE', 'Kinh', 'Long An', '080187200004', TRUE, 'Đại học', 'Quản trị chuỗi cung ứng', 'Trung cấp', 'Tiếng Anh B1', '2016-05-16', 'TP. Thủ Đức, TP. Hồ Chí Minh'),
    ('DEMO-AZC-01', 'Ngô Hải Anh', (SELECT id FROM union_units WHERE code = 'AZC'), 'Kỹ thuật viên', 'Hải Phòng', '2020-11-06', 'MEMBER', 'ACTIVE', 'anh.ngo.demo@gpg.vn', '0913001001', 'AZC', 'Ủy viên BCH', 'Kỹ thuật viên cơ điện', 'MALE', 'Kinh', 'Hải Phòng', '031189300001', TRUE, 'Cao đẳng', 'Cơ điện', 'Trung cấp', 'Tiếng Anh A2', '2019-03-11', 'Hồng Bàng, Hải Phòng'),
    ('DEMO-AZC-02', 'Đinh Hồng Nhung', (SELECT id FROM union_units WHERE code = 'AZC'), 'Nhân viên QA', 'Hải Phòng', '2023-02-14', 'MEMBER', 'ACTIVE', 'nhung.dinh.demo@gpg.vn', '0913001002', 'AZC', 'Đoàn viên', 'Kiểm soát chất lượng', 'FEMALE', 'Kinh', 'Thái Bình', '034194300002', FALSE, 'Đại học', 'Quản lý chất lượng', 'Sơ cấp', 'Tiếng Anh B1', '2022-08-01', 'Kiến An, Hải Phòng'),
    ('DEMO-AZC-03', 'Phan Tuấn Kiệt', (SELECT id FROM union_units WHERE code = 'AZC'), 'Công nhân vận hành', 'Hải Phòng', '2026-06-20', 'MEMBER', 'ACTIVE', 'kiet.phan.demo@gpg.vn', '0913001003', 'AZC', 'Đoàn viên', 'Vận hành máy', 'MALE', 'Kinh', 'Hải Dương', '030199300003', FALSE, 'Trung cấp', 'Cơ khí', NULL, 'Cơ bản', '2026-03-04', 'An Dương, Hải Phòng'),
    ('DEMO-AZC-04', 'Hoàng Mỹ Duyên', (SELECT id FROM union_units WHERE code = 'AZC'), 'Nhân viên HSE', 'Hải Phòng', '2021-05-09', 'MEMBER', 'ACTIVE', 'duyen.hoang.demo@gpg.vn', '0913001004', 'AZC', 'Tổ trưởng công đoàn', 'Chuyên viên an toàn', 'FEMALE', 'Kinh', 'Quảng Ninh', '022192300004', FALSE, 'Đại học', 'An toàn lao động', 'Trung cấp', 'Tiếng Anh B2', '2019-09-23', 'Lê Chân, Hải Phòng'),
    ('DEMO-GPD-01', 'Đỗ Quang Vinh', (SELECT id FROM union_units WHERE code = 'GPD'), 'Chuyên viên kinh doanh', 'Hà Nội', '2020-08-17', 'MEMBER', 'ACTIVE', 'vinh.do.demo@gpg.vn', '0914001001', 'GPD', 'Ủy viên BCH', 'Chuyên viên kinh doanh', 'MALE', 'Kinh', 'Hà Nội', '001188400001', TRUE, 'Đại học', 'Quản trị kinh doanh', 'Trung cấp', 'Tiếng Anh B2', '2018-01-09', 'Cầu Giấy, Hà Nội'),
    ('DEMO-GPD-02', 'Mai Phương Hoa', (SELECT id FROM union_units WHERE code = 'GPD'), 'Chuyên viên nhân sự', 'Hà Nội', '2023-09-01', 'MEMBER', 'ACTIVE', 'hoa.mai.demo@gpg.vn', '0914001002', 'GPD', 'Đoàn viên', 'Chuyên viên C&B', 'FEMALE', 'Kinh', 'Hưng Yên', '033193400002', FALSE, 'Đại học', 'Quản trị nhân lực', 'Sơ cấp', 'Tiếng Anh B1', '2022-12-05', 'Nam Từ Liêm, Hà Nội'),
    ('DEMO-GPD-03', 'Trần Gia Bảo', (SELECT id FROM union_units WHERE code = 'GPD'), 'Nhân viên dịch vụ', 'Hà Nội', '2026-03-18', 'MEMBER', 'ACTIVE', 'bao.tran.demo@gpg.vn', '0914001003', 'GPD', 'Đoàn viên', 'Nhân viên chăm sóc khách hàng', 'MALE', 'Kinh', 'Phú Thọ', '025200400003', FALSE, 'Cao đẳng', 'Dịch vụ khách hàng', NULL, 'Tiếng Anh A2', '2025-12-10', 'Bắc Từ Liêm, Hà Nội'),
    ('DEMO-GPD-04', 'Nguyễn Thanh Hằng', (SELECT id FROM union_units WHERE code = 'GPD'), 'Trưởng nhóm dịch vụ', 'Hà Nội', '2019-04-23', 'MEMBER', 'ACTIVE', 'hang.nguyen.demo@gpg.vn', '0914001004', 'GPD', 'Tổ trưởng công đoàn', 'Quản lý dịch vụ', 'FEMALE', 'Kinh', 'Hà Nam', '035187400004', TRUE, 'Sau đại học', 'Quản trị dịch vụ', 'Cao cấp', 'Tiếng Anh C1', '2015-07-20', 'Thanh Xuân, Hà Nội');

INSERT INTO welfare_records
    (record_code, welfare_type, policy_name, policy_id, union_unit_id, beneficiary_name, event_date,
     deadline, status, amount, standard_amount, document_status, receipt_status, has_image, notes)
VALUES
    ('DEMO-CL-2501-01', 'BIRTHDAY', 'Sinh Nhật', (SELECT id FROM welfare_policies WHERE code = 'CD-03-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Trần Ngọc Mai', '2025-01-18', '2025-01-25', 'COMPLETED', 100000, 100000, 'COMPLETE', 'COMPLETE', TRUE, 'Chúc mừng sinh nhật đoàn viên tháng 1'),
    ('DEMO-CL-2502-01', 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', (SELECT id FROM welfare_policies WHERE code = 'CD-01-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Võ Thu Trang', '2025-02-09', '2025-02-16', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', FALSE, 'Hỗ trợ tang chế từ nguồn công đoàn'),
    ('DEMO-CL-2503-01', 'WEDDING', 'Đám cưới', (SELECT id FROM welfare_policies WHERE code = 'CD-02-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Đinh Hồng Nhung', '2025-03-22', '2025-03-29', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', TRUE, 'Mừng cưới đoàn viên'),
    ('DEMO-CL-2504-01', 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện', (SELECT id FROM welfare_policies WHERE code = 'CD-04-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Mai Phương Hoa', '2025-04-11', '2025-04-18', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', FALSE, 'Thăm hỏi thân nhân nằm viện'),
    ('DEMO-CL-2505-01', 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện phẫu thuật', (SELECT id FROM welfare_policies WHERE code = 'CD-05-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Phạm Quốc Huy', '2025-05-14', '2025-05-21', 'COMPLETED', 1000000, 1000000, 'COMPLETE', 'COMPLETE', TRUE, 'Thăm hỏi sau phẫu thuật'),
    ('DEMO-CL-2506-01', 'CHILDBIRTH', 'Nhân viên nữ sinh con', (SELECT id FROM welfare_policies WHERE code = 'CD-06-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Võ Thu Trang', '2025-06-03', '2025-06-10', 'COMPLETED', 1000000, 1000000, 'COMPLETE', 'COMPLETE', TRUE, 'Nhân viên nữ có thâm niên trên 3 năm'),
    ('DEMO-CL-2507-01', 'CHILDBIRTH', 'Nhân viên nữ sinh con', (SELECT id FROM welfare_policies WHERE code = 'CD-07-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Đinh Hồng Nhung', '2025-07-19', '2025-07-26', 'COMPLETED', 500000, 500000, 'COMPLETE', 'COMPLETE', FALSE, 'Nhân viên nữ đủ điều kiện 2 năm'),
    ('DEMO-CL-2508-01', 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', (SELECT id FROM welfare_policies WHERE code = 'CD-08-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Đỗ Quang Vinh', '2025-08-08', '2025-08-15', 'COMPLETED', 500000, 500000, 'COMPLETE', 'COMPLETE', TRUE, 'Vợ nhân viên nam sinh con, thâm niên trên 2 năm'),
    ('DEMO-CL-2509-01', 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', (SELECT id FROM welfare_policies WHERE code = 'CD-09-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Phạm Quốc Huy', '2025-09-27', '2025-10-04', 'COMPLETED', 200000, 200000, 'COMPLETE', 'COMPLETE', FALSE, 'Vợ nhân viên nam sinh con, thâm niên trên 1 năm'),
    ('DEMO-CL-2510-01', 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', (SELECT id FROM welfare_policies WHERE code = 'CT-10-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Trương Thành Đạt', '2025-10-04', '2025-10-11', 'COMPLETED', 1000000, 1000000, 'COMPLETE', 'COMPLETE', TRUE, 'Công ty hỗ trợ nhân viên trên 1 năm'),
    ('DEMO-CL-2511-01', 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', (SELECT id FROM welfare_policies WHERE code = 'CT-10-02'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Phan Tuấn Kiệt', '2025-11-16', '2025-11-23', 'COMPLETED', 500000, 500000, 'COMPLETE', 'COMPLETE', FALSE, 'Công ty hỗ trợ trường hợp thử việc dưới 6 tháng'),
    ('DEMO-CL-2512-01', 'WEDDING', 'Đám cưới', (SELECT id FROM welfare_policies WHERE code = 'CT-11-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Mai Phương Hoa', '2025-12-20', '2025-12-27', 'COMPLETED', 700000, 700000, 'COMPLETE', 'COMPLETE', TRUE, 'Công ty mừng cưới nhân viên trên 6 tháng'),
    ('DEMO-CL-2601-01', 'WEDDING', 'Đám cưới', (SELECT id FROM welfare_policies WHERE code = 'CT-11-02'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Lê Thảo Vy', '2026-01-10', '2026-01-17', 'COMPLETED', 400000, 400000, 'COMPLETE', 'COMPLETE', TRUE, 'Công ty mừng cưới nhân viên dưới 6 tháng'),
    ('DEMO-CL-2602-01', 'CHILDBIRTH', 'Nhân viên nữ sinh con', (SELECT id FROM welfare_policies WHERE code = 'CT-12-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Võ Thu Trang', '2026-02-14', '2026-02-21', 'COMPLETED', 1000000, 1000000, 'COMPLETE', 'COMPLETE', TRUE, 'Công ty hỗ trợ nhân viên nữ từ 3 năm'),
    ('DEMO-CL-2603-01', 'CHILDBIRTH', 'Nhân viên nữ sinh con', (SELECT id FROM welfare_policies WHERE code = 'CT-13-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Đinh Hồng Nhung', '2026-03-07', '2026-03-14', 'COMPLETED', 500000, 500000, 'COMPLETE', 'COMPLETE', FALSE, 'Công ty hỗ trợ nhân viên nữ từ 2 năm'),
    ('DEMO-CL-2604-01', 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', (SELECT id FROM welfare_policies WHERE code = 'CT-14-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Đỗ Quang Vinh', '2026-04-25', '2026-05-02', 'COMPLETED', 500000, 500000, 'COMPLETE', 'COMPLETE', TRUE, 'Công ty hỗ trợ vợ nhân viên nam từ 2 năm'),
    ('DEMO-CL-2605-01', 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', (SELECT id FROM welfare_policies WHERE code = 'CT-15-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Phạm Quốc Huy', '2026-05-17', '2026-05-24', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', FALSE, 'Công ty hỗ trợ vợ nhân viên nam từ 1 năm'),
    ('DEMO-CL-2606-01', 'BIRTHDAY', 'Sinh Nhật', (SELECT id FROM welfare_policies WHERE code = 'CD-03-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Bùi Khánh Linh', '2026-06-09', '2026-06-16', 'COMPLETED', 100000, 100000, 'COMPLETE', 'COMPLETE', TRUE, 'Sinh nhật đoàn viên mới'),
    ('DEMO-CL-2606-02', 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện', (SELECT id FROM welfare_policies WHERE code = 'CD-04-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Hoàng Mỹ Duyên', '2026-06-21', '2026-06-28', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', FALSE, 'Thăm hỏi đoàn viên nằm viện'),
    ('DEMO-CL-2607-01', 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', (SELECT id FROM welfare_policies WHERE code = 'CD-01-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Nguyễn Thanh Hằng', '2026-07-03', '2026-07-10', 'COMPLETED', 300000, 300000, 'COMPLETE', 'COMPLETE', TRUE, 'Hỗ trợ tang chế từ nguồn công đoàn'),
    ('DEMO-CL-2607-02', 'WEDDING', 'Đám cưới', (SELECT id FROM welfare_policies WHERE code = 'CT-11-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Đặng Minh Tâm', '2026-07-18', '2026-07-25', 'COMPLETED', 700000, 700000, 'COMPLETE', 'COMPLETE', TRUE, 'Mừng cưới từ nguồn công ty'),
    ('DEMO-CL-2608-01', 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện phẫu thuật', (SELECT id FROM welfare_policies WHERE code = 'CD-05-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Nguyễn Đức Long', '2026-08-05', '2026-08-12', 'COMPLETED', 1000000, 1000000, 'COMPLETE', 'COMPLETE', TRUE, 'Thăm hỏi sau phẫu thuật, đã quyết toán'),
    ('DEMO-CL-2608-02', 'CHILDBIRTH', 'Nhân viên nữ sinh con', (SELECT id FROM welfare_policies WHERE code = 'CD-07-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Đinh Hồng Nhung', '2026-08-12', '2026-08-19', 'PENDING_APPROVAL', 500000, 500000, 'COMPLETE', 'INCOMPLETE', TRUE, 'Đang chờ BCH duyệt chi'),
    ('DEMO-CL-2608-03', 'FUNERAL', 'Ma Chay (đối với tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên)', (SELECT id FROM welfare_policies WHERE code = 'CT-10-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Nguyễn Thanh Hằng', '2026-08-18', '2026-08-25', 'IN_PROGRESS', 1000000, 1000000, 'INCOMPLETE', 'INCOMPLETE', FALSE, 'Đang bổ sung giấy xác nhận'),
    ('DEMO-CL-2609-01', 'BIRTHDAY', 'Sinh Nhật', (SELECT id FROM welfare_policies WHERE code = 'CD-03-01'), (SELECT id FROM union_units WHERE code = 'VCS'), 'Lê Thảo Vy', '2026-09-08', '2026-09-15', 'NEW', 100000, 100000, 'INCOMPLETE', 'INCOMPLETE', FALSE, 'Hồ sơ dự kiến quý III'),
    ('DEMO-CL-2610-01', 'WEDDING', 'Đám cưới', (SELECT id FROM welfare_policies WHERE code = 'CD-02-01'), (SELECT id FROM union_units WHERE code = 'GPL'), 'Bùi Khánh Linh', '2026-10-10', '2026-10-17', 'NEW', 300000, 300000, 'INCOMPLETE', 'INCOMPLETE', FALSE, 'Kế hoạch chăm lo quý IV'),
    ('DEMO-CL-2611-01', 'VISIT', 'Nhân viên, tứ thân phụ mẫu, vợ/chồng, con ruột của nhân viên nằm viện', (SELECT id FROM welfare_policies WHERE code = 'CD-04-01'), (SELECT id FROM union_units WHERE code = 'AZC'), 'Trần B', '2026-11-06', '2026-11-13', 'NEW', 300000, 300000, 'INCOMPLETE', 'INCOMPLETE', FALSE, 'Dữ liệu dự kiến để theo dõi kế hoạch năm'),
    ('DEMO-CL-2612-01', 'CHILDBIRTH', 'Vợ Nhân viên nam sinh con', (SELECT id FROM welfare_policies WHERE code = 'CT-15-01'), (SELECT id FROM union_units WHERE code = 'GPD'), 'Trần Gia Bảo', '2026-12-01', '2026-12-08', 'NEW', 300000, 300000, 'INCOMPLETE', 'INCOMPLETE', FALSE, 'Dữ liệu dự kiến quý IV');

UPDATE welfare_records
SET policy_id = (SELECT id FROM welfare_policies WHERE code = 'CD-03-01'),
    policy_name = (SELECT name FROM welfare_policies WHERE code = 'CD-03-01'),
    standard_amount = (SELECT support_amount FROM welfare_policies WHERE code = 'CD-03-01'),
    deadline = '2026-08-20', receipt_status = 'COMPLETE'
WHERE record_code = 'CS-0826-01';

UPDATE welfare_records
SET policy_id = (SELECT id FROM welfare_policies WHERE code = 'CD-05-01'),
    policy_name = (SELECT name FROM welfare_policies WHERE code = 'CD-05-01'),
    standard_amount = (SELECT support_amount FROM welfare_policies WHERE code = 'CD-05-01'),
    deadline = '2026-08-21', receipt_status = 'INCOMPLETE'
WHERE record_code = 'CS-0826-02';

UPDATE welfare_records
SET policy_id = (SELECT id FROM welfare_policies WHERE code = 'CD-01-01'),
    policy_name = (SELECT name FROM welfare_policies WHERE code = 'CD-01-01'),
    standard_amount = (SELECT support_amount FROM welfare_policies WHERE code = 'CD-01-01'),
    deadline = '2026-08-18', receipt_status = 'COMPLETE'
WHERE record_code = 'CS-0826-03';

INSERT INTO labor_cases
    (case_code, received_date, union_unit_id, issue_group, severity, requester_name, employee_code,
     job_title, workplace, start_work_date, source, attachment_note, owner_name, deadline, status,
     description, affected_people, response_date, result_text, overdue_reason, approved_by, approved_at)
VALUES
    ('DEMO-UV-2501-VCS', '2025-01-12', (SELECT id FROM union_units WHERE code = 'VCS'), 'Tiền lương', 'MEDIUM', 'Trần Ngọc Mai', 'DEMO-VCS-01', 'Chuyên viên kế toán', 'VP-TCT', '2019-08-01', 'Phiếu trực tuyến', 'Bảng lương tháng 12', 'Ban Nữ công', '2025-01-24', 'CLOSED', 'Đề nghị làm rõ khoản phụ cấp làm thêm giờ.', 4, '2025-01-20', 'Đã đối soát và điều chỉnh trong kỳ lương kế tiếp.', NULL, 'Nguyễn Văn Minh', '2025-01-21 09:00:00'),
    ('DEMO-UV-2503-GPL', '2025-03-06', (SELECT id FROM union_units WHERE code = 'GPL'), 'An toàn lao động', 'HIGH', 'Nguyễn Đức Long', 'DEMO-GPL-02', 'Nhân viên kho', 'Linh Xuân', '2023-10-02', 'Đường dây nóng', 'Có ảnh khu vực kho', 'Phạm Mai Anh', '2025-03-13', 'CLOSED', 'Khu vực bốc xếp thiếu biển cảnh báo an toàn.', 18, '2025-03-11', 'Đã bổ sung biển cảnh báo và kẻ vạch an toàn.', NULL, 'Lê Hoàng Nam', '2025-03-12 14:00:00'),
    ('DEMO-UV-2506-AZC', '2025-06-18', (SELECT id FROM union_units WHERE code = 'AZC'), 'Bữa ăn ca', 'MEDIUM', 'Đinh Hồng Nhung', 'DEMO-AZC-02', 'Nhân viên QA', 'Hải Phòng', '2022-08-01', 'Họp tổ công đoàn', NULL, 'Vũ Thanh Hương', '2025-06-30', 'CLOSED', 'Đề nghị đa dạng thực đơn ăn ca.', 35, '2025-06-26', 'Nhà cung cấp đã bổ sung thực đơn theo tuần.', NULL, 'Đỗ Minh Tuấn', '2025-06-27 10:30:00'),
    ('DEMO-UV-2509-GPD', '2025-09-04', (SELECT id FROM union_units WHERE code = 'GPD'), 'Chế độ nghỉ phép', 'LOW', 'Mai Phương Hoa', 'DEMO-GPD-02', 'Chuyên viên nhân sự', 'Hà Nội', '2022-12-05', 'Email', NULL, 'Ngô Bích Ngọc', '2025-09-15', 'CLOSED', 'Hỏi quy trình đăng ký nghỉ bù sau công tác.', 2, '2025-09-10', 'Đã hướng dẫn quy trình và biểu mẫu.', NULL, 'Hoàng Quốc Việt', '2025-09-10 16:00:00'),
    ('DEMO-UV-2601-VCS', '2026-01-09', (SELECT id FROM union_units WHERE code = 'VCS'), 'Điều kiện làm việc', 'MEDIUM', 'Lê Thảo Vy', 'DEMO-VCS-03', 'Nhân viên hành chính', 'VP-TCT', '2026-02-12', 'Phiếu trực tuyến', 'Ảnh khu vực làm việc', 'Trần Thu Hà', '2026-01-20', 'CLOSED', 'Đề nghị bổ sung tủ hồ sơ cho bộ phận hành chính.', 6, '2026-01-17', 'Đã bàn giao hai tủ hồ sơ mới.', NULL, 'Nguyễn Văn Minh', '2026-01-18 09:20:00'),
    ('DEMO-UV-2603-GPL', '2026-03-15', (SELECT id FROM union_units WHERE code = 'GPL'), 'Ca làm việc', 'HIGH', 'Trương Thành Đạt', 'DEMO-GPL-04', 'Tổ trưởng vận hành', 'Linh Xuân', '2016-05-16', 'Họp đối thoại', 'Danh sách 22 NLĐ', 'Lê Hoàng Nam', '2026-03-25', 'CLOSED', 'Đề nghị điều chỉnh lịch luân phiên ca đêm.', 22, '2026-03-23', 'Đã thống nhất lịch luân phiên mới từ tháng 4.', NULL, 'Lê Hoàng Nam', '2026-03-24 08:30:00'),
    ('DEMO-UV-2605-AZC', '2026-05-08', (SELECT id FROM union_units WHERE code = 'AZC'), 'Trang bị bảo hộ', 'CRITICAL', 'Ngô Hải Anh', 'DEMO-AZC-01', 'Kỹ thuật viên', 'Hải Phòng', '2019-03-11', 'Đường dây nóng', 'Biên bản hiện trường', 'Đỗ Minh Tuấn', '2026-05-10', 'CLOSED', 'Phát hiện lô găng tay bảo hộ không đạt chuẩn.', 48, '2026-05-09', 'Đã thu hồi và cấp thay thế toàn bộ lô không đạt.', NULL, 'Đỗ Minh Tuấn', '2026-05-09 15:15:00'),
    ('DEMO-UV-2607-GPD', '2026-07-02', (SELECT id FROM union_units WHERE code = 'GPD'), 'Phúc lợi', 'LOW', 'Trần Gia Bảo', 'DEMO-GPD-03', 'Nhân viên dịch vụ', 'Hà Nội', '2025-12-10', 'Phiếu trực tuyến', NULL, 'Ngô Bích Ngọc', '2026-07-14', 'CLOSED', 'Đề nghị công bố lịch chăm lo quý III.', 12, '2026-07-09', 'Đã công bố lịch trên cổng thông tin nội bộ.', NULL, 'Hoàng Quốc Việt', '2026-07-10 11:00:00'),
    ('DEMO-UV-2608-VCS', '2026-08-20', (SELECT id FROM union_units WHERE code = 'VCS'), 'Làm thêm giờ', 'HIGH', 'Phạm Quốc Huy', 'DEMO-VCS-02', 'Kỹ sư hệ thống', 'VP-TCT', '2022-11-15', 'Email', 'Bảng tổng hợp giờ làm', 'Đặng Minh Tâm', '2026-09-03', 'IN_PROGRESS', 'Đề nghị xác nhận giờ trực hệ thống cuối tuần.', 8, NULL, 'Đang đối chiếu dữ liệu chấm công.', NULL, NULL, NULL),
    ('DEMO-UV-2608-GPL', '2026-08-22', (SELECT id FROM union_units WHERE code = 'GPL'), 'Bữa ăn ca', 'MEDIUM', 'Bùi Khánh Linh', 'DEMO-GPL-03', 'Nhân viên chứng từ', 'Linh Xuân', '2026-01-08', 'Khảo sát nhanh', NULL, NULL, NULL, 'CLASSIFYING', 'Phản ánh chất lượng suất ăn ca chiều.', 17, NULL, NULL, NULL, NULL, NULL),
    ('DEMO-UV-2609-AZC', '2026-09-02', (SELECT id FROM union_units WHERE code = 'AZC'), 'Môi trường làm việc', 'MEDIUM', 'Hoàng Mỹ Duyên', 'DEMO-AZC-04', 'Nhân viên HSE', 'Hải Phòng', '2019-09-23', 'Họp tổ công đoàn', 'Phiếu đo nhiệt độ', 'Vũ Thanh Hương', '2026-09-15', 'ASSIGNED', 'Đề nghị kiểm tra nhiệt độ khu vực đóng gói.', 26, NULL, NULL, NULL, NULL, NULL),
    ('DEMO-UV-2610-GPD', '2026-10-05', (SELECT id FROM union_units WHERE code = 'GPD'), 'Đào tạo', 'LOW', 'Mai Phương Hoa', 'DEMO-GPD-02', 'Chuyên viên nhân sự', 'Hà Nội', '2022-12-05', 'Phiếu trực tuyến', NULL, NULL, NULL, 'NEW', 'Đề nghị mở lớp kỹ năng xử lý khách hàng khó.', 14, NULL, NULL, NULL, NULL, NULL);

INSERT INTO union_activities
    (activity_code, name, union_unit_id, event_date, event_time, location, status, objective,
     planned_budget, actual_cost, program_pic, invited_count, employee_group, participant_count,
     participant_list, check_in_count, workers_reached, usefulness_score, quick_feedback,
     actual_content, plan_difference, output_proposal, communication_content, strengths,
     weaknesses, report_completed, document_status, lessons_learned, follow_up_issue,
     follow_up_owner, follow_up_deadline, follow_up_status)
VALUES
    ('DEMO-ACT-2501-VCS', 'Tết sum vầy 2025', (SELECT id FROM union_units WHERE code = 'VCS'), '2025-01-20', '09:00:00', 'Hội trường VP-TCT', 'COMPLETED', 'Chăm lo đoàn viên dịp Tết', 18000000, 17600000, 'Trần Thu Hà', 90, 'Toàn thể đoàn viên', 84, 'Danh sách check-in điện tử', 82, 84, 4.70, '96% người tham dự đánh giá hài lòng', 'Tặng quà và tổ chức giao lưu cuối năm', 'Không thay đổi đáng kể', 'Duy trì gói quà cho đoàn viên khó khăn', 'Tin bài và 24 ảnh nội bộ', 'Tổ chức đúng tiến độ, tỷ lệ tham gia cao', 'Khu vực check-in còn chậm', TRUE, 'COMPLETE', 'Mở hai bàn check-in cho sự kiện đông người', 'Chuẩn hóa danh sách đoàn viên khó khăn', 'Trần Thu Hà', '2025-02-10', 'COMPLETED'),
    ('DEMO-ACT-2504-GPL', 'Tháng Công nhân GPL', (SELECT id FROM union_units WHERE code = 'GPL'), '2025-04-26', '08:00:00', 'Kho Linh Xuân', 'COMPLETED', 'Đối thoại và chăm sóc sức khỏe NLĐ', 15000000, 14750000, 'Phạm Mai Anh', 120, 'Khối kho vận', 108, 'Danh sách 108 người tham dự', 106, 108, 4.50, 'Nhu cầu nổi bật: ca làm và bữa ăn', 'Khám sức khỏe nhanh, đối thoại và minigame an toàn', 'Rút ngắn phần khai mạc 15 phút', 'Đề xuất cải thiện khu nghỉ giữa ca', 'Poster, bài viết và video recap', 'Nội dung sát nhu cầu NLĐ', 'Âm thanh khu vực ngoài trời chưa ổn định', TRUE, 'COMPLETE', 'Cần khảo sát địa điểm trước chương trình', 'Theo dõi phương án khu nghỉ giữa ca', 'Lê Hoàng Nam', '2025-05-20', 'COMPLETED'),
    ('DEMO-ACT-2507-AZC', 'Ngày hội an toàn AZC', (SELECT id FROM union_units WHERE code = 'AZC'), '2025-07-12', '07:30:00', 'Nhà máy Hải Phòng', 'COMPLETED', 'Nâng cao nhận thức an toàn lao động', 12000000, 11300000, 'Hoàng Mỹ Duyên', 150, 'Khối sản xuất', 142, 'Danh sách theo tổ sản xuất', 139, 142, 4.80, 'Các trạm thực hành được đánh giá cao', 'Huấn luyện, thi nhận diện rủi ro và sơ cứu', 'Bổ sung thêm trạm sơ cứu so với kế hoạch', 'Nhân rộng mô hình 5 phút an toàn đầu ca', '32 ảnh và infographic an toàn', 'Tính thực hành cao', 'Thời gian luân chuyển giữa các trạm còn dài', TRUE, 'COMPLETE', 'Chia nhóm nhỏ hơn ở lần tiếp theo', 'Triển khai 5 phút an toàn đầu ca', 'Hoàng Mỹ Duyên', '2025-08-01', 'COMPLETED'),
    ('DEMO-ACT-2510-GPD', 'Kết nối gia đình GPD', (SELECT id FROM union_units WHERE code = 'GPD'), '2025-10-18', '15:00:00', 'Công viên Yên Sở', 'COMPLETED', 'Tăng gắn kết đoàn viên và gia đình', 20000000, 19300000, 'Ngô Bích Ngọc', 130, 'Đoàn viên và gia đình', 118, 'Danh sách gia đình đăng ký', 115, 118, 4.60, 'Mong muốn duy trì hằng năm', 'Trò chơi gia đình, gian hàng thiếu nhi và tư vấn phúc lợi', 'Chuyển địa điểm do thời tiết', 'Tổ chức định kỳ vào quý IV', 'Album ảnh và bản tin nội bộ', 'Không khí gần gũi, đa dạng nhóm tuổi', 'Khu vực gửi xe quá tải', TRUE, 'COMPLETE', 'Cần bố trí sơ đồ giao thông riêng', 'Khảo sát thời điểm tổ chức năm 2026', 'Ngô Bích Ngọc', '2025-11-05', 'COMPLETED'),
    ('DEMO-ACT-2602-VCS', 'Đối thoại đầu năm VCS', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-02-21', '09:00:00', 'Phòng họp tầng 8', 'COMPLETED', 'Nắm bắt nhu cầu và giải đáp chính sách', 7000000, 6500000, 'Đặng Minh Tâm', 70, 'Đại diện các phòng ban', 64, 'Danh sách đại biểu', 63, 64, 4.40, 'Ưu tiên chủ đề làm thêm giờ và phúc lợi', 'Đối thoại trực tiếp theo 5 nhóm chủ đề', 'Gộp hai chủ đề do trùng nội dung', 'Phát hành FAQ sau chương trình', 'Bản tin FAQ và 12 ảnh', 'Lãnh đạo phản hồi trực tiếp', 'Thời lượng hỏi đáp chưa đủ', TRUE, 'COMPLETE', 'Thu câu hỏi trước ít nhất 3 ngày', 'Hoàn thiện FAQ chính sách', 'Đặng Minh Tâm', '2026-03-05', 'COMPLETED'),
    ('DEMO-ACT-2603-GPL', 'Ngày hội sức khỏe GPL', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-03-28', '08:00:00', 'Kho Linh Xuân', 'COMPLETED', 'Tầm soát sức khỏe và tư vấn nghề nghiệp', 13500000, 13100000, 'Võ Thu Trang', 110, 'Khối kho vận', 101, 'Danh sách khám theo khung giờ', 99, 101, 4.70, 'Đánh giá cao phần tư vấn cột sống', 'Tầm soát, tư vấn dinh dưỡng và ergonomics', 'Bổ sung bàn tư vấn cột sống', 'Trang bị ghế hỗ trợ cho vị trí đặc thù', 'Video hướng dẫn và ảnh sự kiện', 'Quy trình phân luồng tốt', 'Thiếu thời gian tư vấn cá nhân', TRUE, 'COMPLETE', 'Tăng số chuyên gia theo nhóm bệnh', 'Khảo sát nhu cầu ghế hỗ trợ', 'Võ Thu Trang', '2026-04-15', 'IN_PROGRESS'),
    ('DEMO-ACT-2605-AZC', 'Tháng Công nhân AZC', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-05-23', '07:30:00', 'Nhà máy Hải Phòng', 'COMPLETED', 'Chăm lo, đối thoại và tôn vinh NLĐ', 22000000, 21600000, 'Vũ Thanh Hương', 180, 'Toàn thể NLĐ nhà máy', 169, 'Danh sách theo phân xưởng', 166, 169, 4.90, '98% đánh giá hữu ích hoặc rất hữu ích', 'Đối thoại, gian hàng phúc lợi và khen thưởng', 'Tăng 20 suất quà so với kế hoạch', 'Duy trì gian hàng tư vấn chính sách hằng quý', 'Thông cáo, poster và 45 ảnh', 'Phủ rộng đối tượng và nội dung thiết thực', 'Chương trình kéo dài hơn 20 phút', TRUE, 'COMPLETE', 'Cần kiểm soát thời lượng sân khấu', 'Lập lịch tư vấn chính sách quý III', 'Vũ Thanh Hương', '2026-06-10', 'COMPLETED'),
    ('DEMO-ACT-2606-GPD', 'Workshop tài chính gia đình', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-06-13', '14:00:00', 'Văn phòng Hà Nội', 'COMPLETED', 'Trang bị kỹ năng quản lý tài chính cá nhân', 8500000, 7900000, 'Mai Phương Hoa', 80, 'Đoàn viên dưới 35 tuổi', 72, 'Danh sách đăng ký', 70, 72, 4.50, 'Nhu cầu cao về bảo hiểm và quỹ khẩn cấp', 'Workshop, hỏi đáp và bài tập lập ngân sách', 'Không thay đổi', 'Xây dựng chuỗi nội dung tài chính ngắn', 'Slide, infographic và 16 ảnh', 'Nội dung dễ áp dụng', 'Cần thêm ví dụ theo nhóm thu nhập', TRUE, 'COMPLETE', 'Khảo sát trước mức thu nhập theo khoảng', 'Biên soạn cẩm nang tài chính cá nhân', 'Mai Phương Hoa', '2026-07-01', 'IN_PROGRESS'),
    ('DEMO-ACT-2607-VCS', 'Hiến máu tình nguyện VCS', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-07-16', '07:00:00', 'Hội trường VP-TCT', 'COMPLETED', 'Lan tỏa trách nhiệm cộng đồng', 10000000, 9400000, 'Trần Ngọc Mai', 120, 'Toàn thể CBNV', 96, 'Danh sách hiến máu', 94, 96, 4.80, 'Thu được 82 đơn vị máu', 'Khám sàng lọc, hiến máu và tư vấn sức khỏe', 'Bổ sung khung giờ buổi chiều', 'Tổ chức hằng năm và mở rộng đối tác', 'Poster và 28 ảnh', 'Phối hợp y tế nhịp nhàng', 'Một số người chờ khám lâu', TRUE, 'COMPLETE', 'Chia khung giờ đăng ký rõ hơn', 'Đánh giá đối tác y tế cho năm sau', 'Trần Ngọc Mai', '2026-08-05', 'COMPLETED'),
    ('DEMO-ACT-2608-GPL', 'Bữa cơm công đoàn GPL', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-08-19', '11:00:00', 'Căng tin Linh Xuân', 'COMPLETED', 'Gắn kết NLĐ khối kho vận', 9000000, 8650000, 'Trương Thành Đạt', 125, 'Nhân viên kho và vận tải', 119, 'Danh sách ca ăn', 116, 119, 4.60, 'Đề nghị tăng món rau và trái cây', 'Bữa cơm, giao lưu và tiếp nhận góp ý', 'Bổ sung quầy trái cây', 'Cải thiện thực đơn ca chiều', '18 ảnh và bài viết nội bộ', 'Tỷ lệ tham gia cao', 'Không gian căng tin hơi chật', TRUE, 'COMPLETE', 'Bố trí theo hai khung giờ', 'Làm việc với nhà cung cấp suất ăn', 'Trương Thành Đạt', '2026-09-05', 'IN_PROGRESS'),
    ('DEMO-ACT-2609-AZC', 'Đối thoại tại nơi làm việc', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-09-24', '08:30:00', 'Nhà máy Hải Phòng', 'APPROVED', 'Tiếp nhận kiến nghị quý III', 8000000, 0, 'Hoàng Mỹ Duyên', 100, 'Đại diện các phân xưởng', 0, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'Poster và thư mời nội bộ', NULL, NULL, FALSE, 'INCOMPLETE', NULL, 'Chuẩn bị bộ câu hỏi theo nhóm chủ đề', 'Hoàng Mỹ Duyên', '2026-10-10', 'NEW'),
    ('DEMO-ACT-2610-GPD', 'Ngày hội gia đình GPD 2026', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-10-17', '14:00:00', 'Công viên Yên Sở', 'PLANNED', 'Gắn kết đoàn viên và gia đình', 22000000, 0, 'Ngô Bích Ngọc', 150, 'Đoàn viên và gia đình', 0, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'Kế hoạch truyền thông đa kênh', NULL, NULL, FALSE, 'INCOMPLETE', NULL, 'Chốt danh sách nhà cung cấp', 'Ngô Bích Ngọc', '2026-09-20', 'IN_PROGRESS'),
    ('DEMO-ACT-2611-VCS', 'Tập huấn cán bộ công đoàn', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-11-07', '08:00:00', 'Hội trường VP-TCT', 'PLANNED', 'Nâng cao kỹ năng nghiệp vụ BCH', 11000000, 0, 'Đặng Minh Tâm', 45, 'BCH và tổ trưởng công đoàn', 0, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'Email, poster và cổng nội bộ', NULL, NULL, FALSE, 'INCOMPLETE', NULL, 'Hoàn thiện tài liệu tình huống', 'Đặng Minh Tâm', '2026-10-25', 'NEW'),
    ('DEMO-ACT-2612-GPL', 'Tổng kết năm GPL', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-12-19', '15:00:00', 'Kho Linh Xuân', 'PLANNED', 'Tổng kết phong trào và ghi nhận đoàn viên', 16000000, 0, 'Võ Thu Trang', 130, 'Toàn thể đoàn viên', 0, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'Kế hoạch bản tin tổng kết', NULL, NULL, FALSE, 'INCOMPLETE', NULL, 'Tổng hợp đề cử khen thưởng', 'Võ Thu Trang', '2026-11-30', 'NEW');

INSERT INTO finance_entries
    (entry_code, union_unit_id, transaction_date, entry_type, category, amount, description, document_number, document_status)
VALUES
    ('DEMO-TC-2501-VCS-T', (SELECT id FROM union_units WHERE code = 'VCS'), '2025-01-05', 'INCOME', 'Đoàn phí', 14500000, 'Thu đoàn phí quý I', 'PT-VCS-2501', 'COMPLETE'),
    ('DEMO-TC-2501-VCS-C', (SELECT id FROM union_units WHERE code = 'VCS'), '2025-01-20', 'EXPENSE', 'Hoạt động', 17600000, 'Chi Tết sum vầy 2025', 'PC-VCS-2501', 'COMPLETE'),
    ('DEMO-TC-2504-GPL-T', (SELECT id FROM union_units WHERE code = 'GPL'), '2025-04-05', 'INCOME', 'Đoàn phí', 13200000, 'Thu đoàn phí quý II', 'PT-GPL-2504', 'COMPLETE'),
    ('DEMO-TC-2504-GPL-C', (SELECT id FROM union_units WHERE code = 'GPL'), '2025-04-26', 'EXPENSE', 'Hoạt động', 14750000, 'Chi Tháng Công nhân GPL', 'PC-GPL-2504', 'COMPLETE'),
    ('DEMO-TC-2507-AZC-T', (SELECT id FROM union_units WHERE code = 'AZC'), '2025-07-04', 'INCOME', 'Đoàn phí', 16800000, 'Thu đoàn phí quý III', 'PT-AZC-2507', 'COMPLETE'),
    ('DEMO-TC-2507-AZC-C', (SELECT id FROM union_units WHERE code = 'AZC'), '2025-07-12', 'EXPENSE', 'Hoạt động', 11300000, 'Chi Ngày hội an toàn AZC', 'PC-AZC-2507', 'COMPLETE'),
    ('DEMO-TC-2510-GPD-T', (SELECT id FROM union_units WHERE code = 'GPD'), '2025-10-06', 'INCOME', 'Đoàn phí', 15100000, 'Thu đoàn phí quý IV', 'PT-GPD-2510', 'COMPLETE'),
    ('DEMO-TC-2510-GPD-C', (SELECT id FROM union_units WHERE code = 'GPD'), '2025-10-18', 'EXPENSE', 'Hoạt động', 19300000, 'Chi Kết nối gia đình GPD', 'PC-GPD-2510', 'COMPLETE'),
    ('DEMO-TC-2601-VCS-T', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-01-05', 'INCOME', 'Đoàn phí', 15200000, 'Thu đoàn phí quý I', 'PT-VCS-2601', 'COMPLETE'),
    ('DEMO-TC-2602-VCS-C', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-02-21', 'EXPENSE', 'Hoạt động', 6500000, 'Chi đối thoại đầu năm', 'PC-VCS-2602', 'COMPLETE'),
    ('DEMO-TC-2602-GPL-T', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-02-05', 'INCOME', 'Đoàn phí', 13800000, 'Thu đoàn phí quý I', 'PT-GPL-2602', 'COMPLETE'),
    ('DEMO-TC-2603-GPL-C', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-03-28', 'EXPENSE', 'Hoạt động', 13100000, 'Chi Ngày hội sức khỏe GPL', 'PC-GPL-2603', 'COMPLETE'),
    ('DEMO-TC-2604-AZC-T', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-04-06', 'INCOME', 'Đoàn phí', 17400000, 'Thu đoàn phí quý II', 'PT-AZC-2604', 'COMPLETE'),
    ('DEMO-TC-2605-AZC-C', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-05-23', 'EXPENSE', 'Hoạt động', 21600000, 'Chi Tháng Công nhân AZC', 'PC-AZC-2605', 'COMPLETE'),
    ('DEMO-TC-2606-GPD-T', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-06-05', 'INCOME', 'Đoàn phí', 15800000, 'Thu đoàn phí quý II', 'PT-GPD-2606', 'COMPLETE'),
    ('DEMO-TC-2606-GPD-C', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-06-13', 'EXPENSE', 'Hoạt động', 7900000, 'Chi workshop tài chính gia đình', 'PC-GPD-2606', 'COMPLETE'),
    ('DEMO-TC-2607-VCS-C', (SELECT id FROM union_units WHERE code = 'VCS'), '2026-07-16', 'EXPENSE', 'Hoạt động', 9400000, 'Chi hiến máu tình nguyện', 'PC-VCS-2607', 'COMPLETE'),
    ('DEMO-TC-2608-GPL-C', (SELECT id FROM union_units WHERE code = 'GPL'), '2026-08-19', 'EXPENSE', 'Hoạt động', 8650000, 'Chi Bữa cơm công đoàn', 'PC-GPL-2608', 'COMPLETE'),
    ('DEMO-TC-2608-AZC-T', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-08-05', 'INCOME', 'Đoàn phí', 17600000, 'Thu đoàn phí tháng 8', 'PT-AZC-2608', 'COMPLETE'),
    ('DEMO-TC-2608-GPD-T', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-08-05', 'INCOME', 'Đoàn phí', 16000000, 'Thu đoàn phí tháng 8', 'PT-GPD-2608', 'COMPLETE'),
    ('DEMO-TC-2608-AZC-C', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-08-13', 'EXPENSE', 'Chăm lo', 500000, 'Tạm ứng hỗ trợ sinh con', 'PC-AZC-2608', 'INCOMPLETE'),
    ('DEMO-TC-2608-GPD-C', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-08-20', 'EXPENSE', 'Chăm lo', 1000000, 'Dự chi hỗ trợ tang chế', 'PC-GPD-2608', 'INCOMPLETE'),
    ('DEMO-TC-2609-AZC-U', (SELECT id FROM union_units WHERE code = 'AZC'), '2026-09-10', 'ADVANCE', 'Hoạt động', 4000000, 'Tạm ứng đối thoại quý III', 'TU-AZC-2609', 'COMPLETE'),
    ('DEMO-TC-2610-GPD-U', (SELECT id FROM union_units WHERE code = 'GPD'), '2026-10-01', 'ADVANCE', 'Hoạt động', 9000000, 'Tạm ứng Ngày hội gia đình 2026', 'TU-GPD-2610', 'INCOMPLETE');

INSERT INTO monthly_reports
    (union_unit_id, report_month, prepared_by, plan_next_month, support_request, status, submitted_at)
VALUES
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2025-03-01', 'Trần Thu Hà', 'Đối thoại quý II và rà soát hồ sơ chăm lo', 'Hỗ trợ tài liệu truyền thông chính sách', 'APPROVED', '2025-04-03 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2025-03-01', 'Phạm Mai Anh', 'Tổ chức Tháng Công nhân', 'Hỗ trợ chuyên gia an toàn', 'APPROVED', '2025-04-04 10:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2025-03-01', 'Vũ Thanh Hương', 'Rà soát an toàn và kế hoạch quý II', 'Không', 'APPROVED', '2025-04-04 14:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2025-03-01', 'Ngô Bích Ngọc', 'Khảo sát nhu cầu gia đình đoàn viên', 'Hỗ trợ mẫu khảo sát', 'APPROVED', '2025-04-05 08:30:00'),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2025-06-01', 'Trần Thu Hà', 'Chương trình sức khỏe quý III', 'Hỗ trợ kết nối đơn vị y tế', 'APPROVED', '2025-07-03 09:10:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2025-06-01', 'Phạm Mai Anh', 'Theo dõi cải thiện khu nghỉ giữa ca', 'Hỗ trợ dự toán', 'APPROVED', '2025-07-03 15:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2025-06-01', 'Vũ Thanh Hương', 'Ngày hội an toàn AZC', 'Hỗ trợ truyền thông', 'APPROVED', '2025-07-04 08:40:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2025-06-01', 'Ngô Bích Ngọc', 'Tập huấn cán bộ tổ công đoàn', 'Không', 'APPROVED', '2025-07-04 13:00:00'),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2025-09-01', 'Trần Thu Hà', 'Rà soát ngân sách chăm lo quý IV', 'Không', 'APPROVED', '2025-10-03 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2025-09-01', 'Phạm Mai Anh', 'Đối thoại lịch ca cuối năm', 'Hỗ trợ dữ liệu chấm công', 'APPROVED', '2025-10-03 09:30:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2025-09-01', 'Vũ Thanh Hương', 'Tổng kết sáng kiến an toàn', 'Không', 'APPROVED', '2025-10-04 08:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2025-09-01', 'Ngô Bích Ngọc', 'Kết nối gia đình GPD', 'Hỗ trợ hậu cần', 'APPROVED', '2025-10-04 10:00:00'),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2025-12-01', 'Trần Thu Hà', 'Tết sum vầy 2026', 'Hỗ trợ danh sách hoàn cảnh khó khăn', 'APPROVED', '2026-01-05 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2025-12-01', 'Phạm Mai Anh', 'Kế hoạch chăm lo quý I/2026', 'Không', 'APPROVED', '2026-01-05 10:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2025-12-01', 'Vũ Thanh Hương', 'Rà soát bảo hộ đầu năm', 'Hỗ trợ tiêu chuẩn kiểm định', 'APPROVED', '2026-01-06 08:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2025-12-01', 'Ngô Bích Ngọc', 'Đối thoại đầu năm 2026', 'Không', 'APPROVED', '2026-01-06 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2026-03-01', 'Trần Thu Hà', 'Hiến máu tình nguyện và chăm lo quý II', 'Hỗ trợ kết nối bệnh viện', 'APPROVED', '2026-04-03 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2026-03-01', 'Phạm Mai Anh', 'Tháng Công nhân và đối thoại ca làm', 'Hỗ trợ chuyên gia đối thoại', 'APPROVED', '2026-04-03 10:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2026-03-01', 'Vũ Thanh Hương', 'Kiểm tra bảo hộ và Tháng Công nhân', 'Hỗ trợ truyền thông an toàn', 'APPROVED', '2026-04-04 08:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2026-03-01', 'Ngô Bích Ngọc', 'Workshop tài chính gia đình', 'Hỗ trợ báo cáo viên', 'APPROVED', '2026-04-04 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2026-06-01', 'Trần Thu Hà', 'Hiến máu và đối thoại quý III', 'Không', 'APPROVED', '2026-07-03 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2026-06-01', 'Phạm Mai Anh', 'Bữa cơm công đoàn và khảo sát suất ăn', 'Hỗ trợ khảo sát nhà cung cấp', 'APPROVED', '2026-07-03 10:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2026-06-01', 'Vũ Thanh Hương', 'Đối thoại quý III', 'Không', 'APPROVED', '2026-07-04 08:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2026-06-01', 'Ngô Bích Ngọc', 'Chuẩn bị Ngày hội gia đình', 'Hỗ trợ địa điểm', 'APPROVED', '2026-07-04 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2026-08-01', 'Vũ Thanh Hương', 'Đối thoại tại nơi làm việc quý III', 'Hỗ trợ bộ câu hỏi khảo sát', 'SUBMITTED', '2026-08-29 09:00:00'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2026-08-01', 'Ngô Bích Ngọc', 'Chốt kế hoạch Ngày hội gia đình', 'Hỗ trợ truyền thông liên đơn vị', 'DRAFT', NULL),
    ((SELECT id FROM union_units WHERE code = 'VCS'), '2026-09-01', 'Trần Thu Hà', 'Tập huấn cán bộ công đoàn quý IV', 'Hỗ trợ giảng viên', 'DRAFT', NULL),
    ((SELECT id FROM union_units WHERE code = 'GPL'), '2026-09-01', 'Phạm Mai Anh', 'Tổng kết năm và rà soát chăm lo', 'Không', 'DRAFT', NULL),
    ((SELECT id FROM union_units WHERE code = 'AZC'), '2026-09-01', 'Vũ Thanh Hương', 'Chương trình an toàn cuối năm', 'Hỗ trợ tài liệu', 'DRAFT', NULL),
    ((SELECT id FROM union_units WHERE code = 'GPD'), '2026-09-01', 'Ngô Bích Ngọc', 'Ngày hội gia đình và tổng kết KPI', 'Không', 'DRAFT', NULL);

UPDATE monthly_reports
SET plan_next_month = 'Tập huấn cán bộ công đoàn và chăm lo quý III',
    support_request = 'Hỗ trợ bộ tài liệu chính sách chăm lo',
    status = 'SUBMITTED', submitted_at = '2026-08-29 08:30:00'
WHERE union_unit_id = (SELECT id FROM union_units WHERE code = 'VCS') AND report_month = '2026-08-01';

UPDATE monthly_reports
SET plan_next_month = 'Bữa cơm công đoàn và khảo sát suất ăn',
    support_request = 'Hỗ trợ biểu mẫu khảo sát',
    status = 'SUBMITTED', submitted_at = '2026-08-29 10:00:00'
WHERE union_unit_id = (SELECT id FROM union_units WHERE code = 'GPL') AND report_month = '2026-08-01';

INSERT INTO pulse_surveys
    (survey_code, title, union_unit_id, question_text, start_date, end_date, status, target_responses)
VALUES
    ('DEMO-KS-2503-VCS', 'Mức độ hài lòng quý I/2025 - VCS', (SELECT id FROM union_units WHERE code = 'VCS'), 'Bạn hài lòng thế nào với hoạt động công đoàn quý I?', '2025-03-01', '2025-03-20', 'CLOSED', 5),
    ('DEMO-KS-2506-GPL', 'Nhu cầu sau Tháng Công nhân - GPL', (SELECT id FROM union_units WHERE code = 'GPL'), 'Nội dung nào cần được ưu tiên trong quý III?', '2025-06-01', '2025-06-20', 'CLOSED', 5),
    ('DEMO-KS-2509-AZC', 'Khảo sát an toàn quý III - AZC', (SELECT id FROM union_units WHERE code = 'AZC'), 'Bạn đánh giá điều kiện an toàn tại nơi làm việc thế nào?', '2025-09-01', '2025-09-20', 'CLOSED', 5),
    ('DEMO-KS-2512-GPD', 'Nhu cầu chăm lo năm 2026 - GPD', (SELECT id FROM union_units WHERE code = 'GPD'), 'Chính sách chăm lo nào bạn quan tâm nhất trong năm tới?', '2025-12-01', '2025-12-20', 'CLOSED', 5),
    ('DEMO-KS-2603-VCS', 'Đối thoại đầu năm - VCS', (SELECT id FROM union_units WHERE code = 'VCS'), 'Bạn đánh giá chất lượng phản hồi tại buổi đối thoại?', '2026-03-01', '2026-03-20', 'CLOSED', 5),
    ('DEMO-KS-2606-GPL', 'Sức khỏe nghề nghiệp - GPL', (SELECT id FROM union_units WHERE code = 'GPL'), 'Nội dung sức khỏe nào cần tư vấn thêm?', '2026-06-01', '2026-06-20', 'CLOSED', 5),
    ('DEMO-KS-2608-AZC', 'Tiếng nói NLĐ tháng 8 - AZC', (SELECT id FROM union_units WHERE code = 'AZC'), 'Bạn mong muốn cải thiện điều gì trong quý III?', '2026-08-01', '2026-09-15', 'ACTIVE', 8),
    ('DEMO-KS-2608-GPD', 'Nhu cầu Ngày hội gia đình - GPD', (SELECT id FROM union_units WHERE code = 'GPD'), 'Bạn ưu tiên nội dung nào cho Ngày hội gia đình?', '2026-08-10', '2026-09-10', 'ACTIVE', 8);

INSERT INTO pulse_survey_responses
    (survey_id, rating, need_category, suggestion, anonymous, respondent_name, submitted_on)
VALUES
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2503-VCS'), 5, 'Phúc lợi', 'Công bố lịch chăm lo sớm hơn.', TRUE, NULL, '2025-03-05'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2503-VCS'), 4, 'Đối thoại', 'Tăng thời lượng hỏi đáp.', FALSE, 'Trần Ngọc Mai', '2025-03-08'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2503-VCS'), 5, 'Hoạt động', 'Duy trì hoạt động kết nối liên phòng.', TRUE, NULL, '2025-03-12'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2506-GPL'), 4, 'Điều kiện làm việc', 'Cải thiện khu nghỉ giữa ca.', TRUE, NULL, '2025-06-04'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2506-GPL'), 4, 'Bữa ăn ca', 'Đa dạng thực đơn theo tuần.', FALSE, 'Nguyễn Đức Long', '2025-06-07'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2506-GPL'), 5, 'Sức khỏe', 'Tổ chức khám cột sống định kỳ.', TRUE, NULL, '2025-06-10'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2509-AZC'), 5, 'An toàn lao động', 'Duy trì thực hành nhận diện rủi ro.', TRUE, NULL, '2025-09-03'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2509-AZC'), 4, 'Bảo hộ', 'Công bố lịch kiểm định bảo hộ.', FALSE, 'Hoàng Mỹ Duyên', '2025-09-07'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2509-AZC'), 5, 'Đào tạo', 'Thêm tình huống sơ cứu thực tế.', TRUE, NULL, '2025-09-11'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2512-GPD'), 5, 'Gia đình', 'Ưu tiên hoạt động cho con đoàn viên.', TRUE, NULL, '2025-12-05'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2512-GPD'), 4, 'Sức khỏe', 'Có gói khám sức khỏe gia đình.', FALSE, 'Mai Phương Hoa', '2025-12-08'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2512-GPD'), 4, 'Phúc lợi', 'Có cẩm nang chính sách dễ tra cứu.', TRUE, NULL, '2025-12-12'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2603-VCS'), 5, 'Đối thoại', 'Phần trả lời rõ ràng và thực tế.', FALSE, 'Đặng Minh Tâm', '2026-03-05'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2603-VCS'), 4, 'Làm thêm giờ', 'Cần FAQ về cách tính giờ trực.', TRUE, NULL, '2026-03-09'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2603-VCS'), 5, 'Phúc lợi', 'Danh mục chăm lo đã dễ hiểu hơn.', TRUE, NULL, '2026-03-12'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2606-GPL'), 5, 'Sức khỏe', 'Tư vấn ergonomics rất hữu ích.', FALSE, 'Võ Thu Trang', '2026-06-04'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2606-GPL'), 4, 'Sức khỏe', 'Mong có tư vấn dinh dưỡng theo ca.', TRUE, NULL, '2026-06-08'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2606-GPL'), 4, 'Điều kiện làm việc', 'Cần thêm ghế hỗ trợ lưng.', TRUE, NULL, '2026-06-11'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-AZC'), 5, 'An toàn lao động', 'Duy trì 5 phút an toàn đầu ca.', FALSE, 'Ngô Hải Anh', '2026-08-05'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-AZC'), 3, 'Môi trường làm việc', 'Khu đóng gói cần giảm nhiệt.', TRUE, NULL, '2026-08-10'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-AZC'), 4, 'Bữa ăn ca', 'Tăng lựa chọn món rau.', TRUE, NULL, '2026-08-18'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-GPD'), 5, 'Gia đình', 'Có khu trò chơi cho trẻ nhỏ.', FALSE, 'Nguyễn Thanh Hằng', '2026-08-14'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-GPD'), 4, 'Sức khỏe', 'Bổ sung tư vấn dinh dưỡng gia đình.', TRUE, NULL, '2026-08-20'),
    ((SELECT id FROM pulse_surveys WHERE survey_code = 'DEMO-KS-2608-GPD'), 4, 'Hoạt động', 'Nên chia khung giờ theo độ tuổi trẻ.', TRUE, NULL, '2026-08-26');

INSERT INTO integration_runs
    (integration_type, status, file_name, total_rows, successful_rows, failed_rows, started_by, completed_at, error_summary)
VALUES
    ('HR_IMPORT', 'COMPLETED', 'HR_Master_2025_Q4.xlsx', 20, 20, 0, 'admin', '2026-01-05 09:15:00', NULL),
    ('FINANCE_IMPORT', 'COMPLETED', 'Tai_Chinh_2026_Q1.xlsx', 8, 8, 0, 'admin', '2026-04-04 14:20:00', NULL),
    ('MEMBERS_IMPORT', 'PARTIAL', 'Danh_Sach_Doan_Vien_2026_06.xlsx', 18, 16, 2, 'admin', '2026-06-30 16:10:00', '2 dòng thiếu mã nhân viên hoặc mã CĐCS'),
    ('WELFARE_IMPORT', 'COMPLETED', 'Cham_Lo_2026_08.xlsx', 7, 7, 0, 'admin', '2026-08-22 10:00:00', NULL),
    ('ACTIVITIES_IMPORT', 'COMPLETED', 'Hoat_Dong_Quy_III_2026.xlsx', 4, 4, 0, 'admin', '2026-08-25 13:30:00', NULL),
    ('REPORTS_IMPORT', 'PARTIAL', 'Bao_Cao_M01_2026_08.xlsx', 4, 3, 1, 'admin', '2026-08-29 17:00:00', 'GPD đang ở trạng thái dự thảo');

INSERT INTO member_changes (member_id, change_type, effective_date, description, recorded_by)
VALUES
    ((SELECT id FROM members WHERE employee_code = 'DEMO-VCS-01'), 'Bổ nhiệm BCH', '2025-01-01', 'Bổ nhiệm Ủy viên BCH nhiệm kỳ 2025-2030.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPL-04'), 'Bổ nhiệm tổ trưởng', '2025-02-01', 'Phân công Tổ trưởng công đoàn khối vận hành.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-AZC-04'), 'Thay đổi chức danh', '2025-07-01', 'Chuyển sang vị trí chuyên viên an toàn lao động.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPD-02'), 'Gia nhập công đoàn', '2023-09-01', 'Hoàn tất hồ sơ gia nhập công đoàn.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-VCS-03'), 'Gia nhập công đoàn', '2026-05-15', 'Hoàn tất đơn tự nguyện gia nhập công đoàn.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPL-03'), 'Gia nhập công đoàn', '2026-04-05', 'Được duyệt kết nạp đoàn viên.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-AZC-03'), 'Gia nhập công đoàn', '2026-06-20', 'Được duyệt kết nạp đoàn viên.', 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPD-03'), 'Cập nhật nơi làm việc', '2026-03-01', 'Điều chuyển sang bộ phận dịch vụ khách hàng Hà Nội.', 'admin');

INSERT INTO member_documents
    (member_id, document_type, file_name, content_type, file_size, file_data, uploaded_by)
VALUES
    ((SELECT id FROM members WHERE employee_code = 'DEMO-VCS-03'), 'JOIN_APPLICATION', 'don-gia-nhap-demo-vcs-03.txt', 'text/plain', 46, CAST('Đơn gia nhập công đoàn - dữ liệu mẫu VCS-03' AS BINARY), 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPL-04'), 'DECISION', 'quyet-dinh-to-truong-demo-gpl-04.txt', 'text/plain', 51, CAST('Quyết định tổ trưởng công đoàn - dữ liệu mẫu GPL-04' AS BINARY), 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-AZC-04'), 'BCH_DOCUMENT', 'phan-cong-bch-demo-azc-04.txt', 'text/plain', 45, CAST('Phân công BCH công đoàn - dữ liệu mẫu AZC-04' AS BINARY), 'admin'),
    ((SELECT id FROM members WHERE employee_code = 'DEMO-GPD-02'), 'JOIN_APPLICATION', 'don-gia-nhap-demo-gpd-02.txt', 'text/plain', 46, CAST('Đơn gia nhập công đoàn - dữ liệu mẫu GPD-02' AS BINARY), 'admin');

INSERT INTO welfare_documents
    (welfare_record_id, document_type, file_name, content_type, file_size, file_data, uploaded_by)
VALUES
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2502-01'), 'SUPPORTING_DOCUMENT', 'xac-nhan-tang-che-demo.txt', 'text/plain', 43, CAST('Giấy xác nhận tang chế - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2503-01'), 'IMAGE', 'anh-trao-qua-cuoi-demo.txt', 'text/plain', 39, CAST('Ảnh trao quà cưới - dữ liệu mô phỏng' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2505-01'), 'RECEIPT', 'bien-nhan-tham-hoi-demo.txt', 'text/plain', 39, CAST('Biên nhận thăm hỏi - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2506-01'), 'SUPPORTING_DOCUMENT', 'giay-khai-sinh-demo.txt', 'text/plain', 37, CAST('Giấy khai sinh - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2510-01'), 'RECEIPT', 'bien-nhan-ho-tro-tang-che-demo.txt', 'text/plain', 45, CAST('Biên nhận hỗ trợ tang chế - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2512-01'), 'IMAGE', 'anh-mung-cuoi-gpd-demo.txt', 'text/plain', 40, CAST('Ảnh mừng cưới GPD - dữ liệu mô phỏng' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2602-01'), 'SUPPORTING_DOCUMENT', 'giay-khai-sinh-gpl-demo.txt', 'text/plain', 41, CAST('Giấy khai sinh GPL - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2604-01'), 'RECEIPT', 'bien-nhan-sinh-con-gpd-demo.txt', 'text/plain', 43, CAST('Biên nhận hỗ trợ sinh con - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2607-02'), 'IMAGE', 'anh-trao-qua-cuoi-vcs-demo.txt', 'text/plain', 41, CAST('Ảnh trao quà cưới VCS - dữ liệu mô phỏng' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2608-01'), 'RECEIPT', 'bien-nhan-phau-thuat-demo.txt', 'text/plain', 43, CAST('Biên nhận hỗ trợ phẫu thuật - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2608-02'), 'SUPPORTING_DOCUMENT', 'ho-so-sinh-con-azc-demo.txt', 'text/plain', 42, CAST('Hồ sơ sinh con AZC - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM welfare_records WHERE record_code = 'DEMO-CL-2608-03'), 'SUPPORTING_DOCUMENT', 'ho-so-tang-che-gpd-demo.txt', 'text/plain', 43, CAST('Hồ sơ tang chế GPD còn thiếu xác nhận' AS BINARY), 'admin');

INSERT INTO activity_media
    (activity_id, media_type, title, file_name, content_type, file_size, file_data, uploaded_by)
VALUES
    ((SELECT id FROM union_activities WHERE activity_code = 'DEMO-ACT-2501-VCS'), 'DOCUMENT', 'Báo cáo Tết sum vầy', 'bao-cao-tet-sum-vay-demo.txt', 'text/plain', 41, CAST('Báo cáo Tết sum vầy - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM union_activities WHERE activity_code = 'DEMO-ACT-2507-AZC'), 'PHOTO', 'Ảnh Ngày hội an toàn', 'anh-ngay-hoi-an-toan-demo.txt', 'text/plain', 42, CAST('Ảnh Ngày hội an toàn - dữ liệu mô phỏng' AS BINARY), 'admin'),
    ((SELECT id FROM union_activities WHERE activity_code = 'DEMO-ACT-2605-AZC'), 'DOCUMENT', 'Báo cáo Tháng Công nhân', 'bao-cao-thang-cong-nhan-demo.txt', 'text/plain', 45, CAST('Báo cáo Tháng Công nhân - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM union_activities WHERE activity_code = 'DEMO-ACT-2608-GPL'), 'PHOTO', 'Ảnh Bữa cơm công đoàn', 'anh-bua-com-cong-doan-demo.txt', 'text/plain', 43, CAST('Ảnh Bữa cơm công đoàn - dữ liệu mô phỏng' AS BINARY), 'admin');

INSERT INTO finance_documents
    (finance_entry_id, file_name, content_type, file_size, file_data, uploaded_by)
VALUES
    ((SELECT id FROM finance_entries WHERE entry_code = 'DEMO-TC-2501-VCS-C'), 'phieu-chi-tet-sum-vay-demo.txt', 'text/plain', 39, CAST('Phiếu chi Tết sum vầy - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM finance_entries WHERE entry_code = 'DEMO-TC-2504-GPL-C'), 'phieu-chi-thang-cong-nhan-demo.txt', 'text/plain', 44, CAST('Phiếu chi Tháng Công nhân - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM finance_entries WHERE entry_code = 'DEMO-TC-2605-AZC-C'), 'quyet-toan-thang-cong-nhan-demo.txt', 'text/plain', 46, CAST('Quyết toán Tháng Công nhân - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM finance_entries WHERE entry_code = 'DEMO-TC-2606-GPD-C'), 'phieu-chi-workshop-demo.txt', 'text/plain', 40, CAST('Phiếu chi workshop - dữ liệu mẫu' AS BINARY), 'admin');

INSERT INTO document_library
    (union_unit_id, category, title, description, file_name, content_type, file_size, file_data, uploaded_by)
VALUES
    ((SELECT id FROM union_units WHERE code = 'VCS'), 'Chính sách chăm lo', 'Bảng chế độ hỗ trợ phúc lợi', 'Danh mục 17 mức hỗ trợ dùng thống nhất trong dữ liệu mẫu.', 'bang-che-do-ho-tro-phuc-loi-demo.txt', 'text/plain', 58, CAST('Danh mục chính sách chăm lo theo file Excel nguồn' AS BINARY), 'admin'),
    ((SELECT id FROM union_units WHERE code = 'GPL'), 'Biểu mẫu', 'Mẫu đề nghị chăm lo', 'Biểu mẫu mô phỏng dùng cho hồ sơ chăm lo.', 'mau-de-nghi-cham-lo-demo.txt', 'text/plain', 44, CAST('Mẫu đề nghị chăm lo - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM union_units WHERE code = 'AZC'), 'An toàn lao động', 'Checklist an toàn đầu ca', 'Tài liệu mô phỏng phục vụ Ngày hội an toàn.', 'checklist-an-toan-demo.txt', 'text/plain', 43, CAST('Checklist an toàn đầu ca - dữ liệu mẫu' AS BINARY), 'admin'),
    ((SELECT id FROM union_units WHERE code = 'GPD'), 'Hoạt động', 'Kế hoạch Ngày hội gia đình', 'Kế hoạch mô phỏng cho hoạt động quý IV/2026.', 'ke-hoach-ngay-hoi-gia-dinh-demo.txt', 'text/plain', 47, CAST('Kế hoạch Ngày hội gia đình - dữ liệu mẫu' AS BINARY), 'admin');

COMMIT;
