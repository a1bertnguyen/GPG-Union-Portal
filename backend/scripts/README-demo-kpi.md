# Dùng dữ liệu mẫu KPI

Bộ mẫu này chỉ dành cho máy local. Không chạy trên cơ sở dữ liệu sản xuất.

## Cách chạy

Mở MySQL Workbench, chọn database `union_portal`, mở file `demo-kpi.sql` và bấm **Run**.

Sau đó vào màn **KPI**, chọn:

- CĐCS: `CĐCS DEMO KPI`
- Kỳ: `Năm 2025`

Nếu hệ thống yêu cầu tính KPI, bấm nút **Tính KPI** rồi mở bảng kết quả đơn giản.

## Kết quả dễ nhìn

- Sinh nhật: **8 / 10 = 80%** (8 người đã hoàn tất, 2 người đang chờ).
- Kiến nghị: **4 / 5 = 80%**.
- Hoạt động: **4 / 5 = 80%**.
- Báo cáo tháng: **10 / 12 = 83,3%**.
- Chứng từ tài chính: **5 / 6 = 83,3%**.

Nói ngắn gọn: số bên trái là **đã làm**, số bên phải là **tổng cần làm**.

## Xóa dữ liệu mẫu

Khi không cần nữa, mở và chạy `cleanup-demo-kpi.sql`. Script chỉ xóa CĐCS có mã `DEMO-KPI-2025` và các bản ghi thuộc CĐCS đó.
