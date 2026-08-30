# Dữ liệu mock toàn hệ thống

`full-demo-data.sql` bổ sung dữ liệu trình diễn cho 4 CĐCS trong giai đoạn 2025–2026. Danh mục chăm lo dùng đúng 17 chính sách đã được Flyway tạo ở migration V12, tương ứng với file `Bang_Che_Do_Ho_Tro_Phuc_Loi.xlsx`.

Sau khi Flyway đã chạy đến V17 trên một database mới, thực thi script bằng tài khoản có quyền ghi vào schema ứng dụng:

```powershell
mysql --default-character-set=utf8mb4 -u <user> -p <database> < backend/mock-data/full-demo-data.sql
```

Script được dành cho môi trường demo/local và chỉ nên chạy một lần trên mỗi database. Dữ liệu gồm:

- 20 hồ sơ NLĐ/đoàn viên của VCS, GPL, AZC và GPD.
- 31 hồ sơ chăm lo, phủ đủ 17 mức chính sách Công đoàn/Công ty và nhiều trạng thái chứng từ.
- 15 vụ việc, 17 hoạt động, 28 phiếu tài chính và 32 báo cáo tháng.
- 10 khảo sát với 28 phản hồi, lịch sử tích hợp, biến động đoàn viên và tài liệu minh chứng.
