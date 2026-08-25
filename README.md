# GPG Union Portal — Giai đoạn 1–3

MVP số hóa dữ liệu công đoàn bằng Spring Boot, React và MySQL. Giai đoạn này tập trung vào dữ liệu nền, nhập liệu nghiệp vụ, dashboard và báo cáo tháng.

## Phạm vi đã triển khai

- CĐCS/BCH: thông tin đơn vị, nhiệm kỳ, quyết định và đầu mối.
- Đoàn viên: hồ sơ, trạng thái tham gia, nơi làm việc, bộ lọc và nhập/xuất CSV tương thích Excel.
- Chăm lo: loại chính sách, người thụ hưởng, số tiền, trạng thái và chứng từ.
- Kiến nghị/vụ việc: mức độ, PIC, deadline, trạng thái, kết quả và lý do quá hạn.
- Hoạt động: kế hoạch, ngân sách, chi phí, người tham dự và báo cáo sau chương trình.
- Tài chính nội bộ: nhập phiếu thu/chi, kiểm tra chứng từ và tính tổng thu, tổng chi, số dư.
- Báo cáo M01: tổng hợp dữ liệu tháng theo từng CĐCS hoặc toàn hệ thống và xuất CSV.
- Dashboard: sáu màn hình độc lập cho điều hành, chăm lo, vụ việc, hoạt động, tài chính và tiếng nói NLĐ.

## Giai đoạn 2 thử nghiệm

- Tạo và đóng chiến dịch khảo sát nhanh theo từng CĐCS.
- NLĐ gửi đánh giá 1–5, chọn nhóm nhu cầu và có thể phản hồi ẩn danh.
- Dashboard Employee Voice tổng hợp tỷ lệ phản hồi, điểm kết nối, tỷ lệ kiến nghị có kết quả, điểm hữu ích hoạt động và top nhu cầu.
- Cảnh báo tự động khi tỷ lệ phản hồi dưới 60%, điểm kết nối dưới 3,5, kiến nghị có phản hồi dưới 90% hoặc hoạt động có điểm hữu ích thấp.

## Giai đoạn 3 — tích hợp dữ liệu nội bộ

- Nhập HR Master bằng CSV, cập nhật theo mã nhân viên và mã CĐCS.
- Nhập phiếu thu/chi kế toán bằng CSV, cập nhật theo mã phiếu.
- Xuất CSV tài chính theo tháng/CĐCS để đối soát thủ công hoặc dùng làm tệp mẫu.
- Lưu lịch sử từng lượt nhập: loại dữ liệu, tệp, người thực hiện, số dòng thành công/thất bại và chi tiết lỗi.
- Chỉ `ADMIN` được dùng màn hình **Tích hợp dữ liệu**.

> Tài chính không kết nối ngân hàng, ví điện tử, cổng thanh toán hoặc dịch vụ chuyển tiền. Hệ thống chỉ lưu dữ liệu người dùng nhập và thực hiện phép tính nội bộ.

## Đăng nhập nội bộ và phân quyền

Toàn bộ API nghiệp vụ được bảo vệ bằng JWT ký HMAC-SHA256. Mật khẩu admin được lưu dưới dạng BCrypt; JWT có thời hạn mặc định 8 giờ và frontend chỉ giữ token trong `sessionStorage`.

Tài khoản khởi tạo dành cho local/dev:

- `ADMIN`: `admin` / `Admin@123!` — toàn quyền, xem toàn hệ thống, quản lý CĐCS, tài khoản và tích hợp dữ liệu.
- `USER`: `user.vcs` / `User@123!` — vận hành đoàn viên, chăm lo, vụ việc, hoạt động, khảo sát, tài chính nội bộ và báo cáo trong CĐCS `VCS`.

Backend lấy phạm vi CĐCS trực tiếp từ claim `unitId` trong JWT. Tham số hoặc payload do USER gửi lên không thể dùng để xem hay thay đổi dữ liệu của đơn vị khác. Quản lý CĐCS, quản lý tài khoản, nhập CSV hàng loạt và tích hợp dữ liệu chỉ dành cho ADMIN.

ADMIN có màn hình **Tài khoản & phân quyền** để tạo tài khoản, gán USER vào CĐCS, đổi vai trò, khóa/mở tài khoản và đặt lại mật khẩu. Hệ thống không cho khóa/hạ quyền ADMIN đang đăng nhập hoặc làm mất ADMIN hoạt động cuối cùng.

Trước khi triển khai, sao chép `.env.example` thành `.env`, thay `JWT_SECRET` bằng chuỗi ngẫu nhiên tối thiểu 32 byte và đặt mật khẩu mạnh cho cả hai tài khoản. Các biến `ADMIN_*`/`USER_*` chỉ tạo tài khoản ở lần khởi động đầu tiên khi username chưa tồn tại; chúng không tự đổi mật khẩu của tài khoản đã có. `USER_UNIT_CODE` phải trùng mã CĐCS đã tồn tại.

## Chạy nhanh bằng Docker

Yêu cầu: Docker Desktop đang chạy.

Tạo cấu hình môi trường trước khi chạy thật:

```powershell
Copy-Item .env.example .env
# Chỉnh JWT_SECRET và ADMIN_PASSWORD trong .env
```

```powershell
docker compose up --build
```

Sau khi các container healthy:

- Giao diện: http://localhost:3637
- Backend API: http://localhost:3638/api
- Health check: http://localhost:3638/actuator/health
- MySQL: localhost:3306, database `union_portal`

Dừng hệ thống:

```powershell
docker compose down
```

Xóa cả dữ liệu MySQL để khởi tạo lại dữ liệu mẫu:

```powershell
docker compose down -v
```

## Chạy cho phát triển

Backend cần MySQL đang chạy:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

## Kiến trúc backend

Backend được tổ chức theo mô hình MVC phân tầng dưới package `vn.gpg.unionportal`:

- `controller`: khai báo REST endpoint, nhận/validate request và trả response.
- `service`: xử lý nghiệp vụ, phân quyền theo CĐCS và quản lý transaction.
- `repository`: truy cập dữ liệu qua Spring Data JPA.
- `model`: các JPA entity và enum miền nghiệp vụ.
- `dto`: request/response model của API.
- `mapper`: chuyển DTO sang entity.
- `exception`: exception nghiệp vụ và bộ xử lý lỗi dùng chung.
- `config`: bảo mật, CORS và khởi tạo tài khoản.

Cấu hình Spring Boot nằm tại `backend/src/main/resources/application.properties`; cấu hình test nằm tại `backend/src/test/resources/application.properties`. Có thể ghi đè kết nối database bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và các thiết lập khác bằng những biến môi trường được khai báo trong file này.

## Nhập dữ liệu từ Excel

Các màn hình có dữ liệu nhập liệu đều có nút tải **Mẫu Excel** và **Nhập Excel**: CĐCS, đoàn viên, chăm lo, vụ việc, hoạt động, tài chính, báo cáo tháng, khảo sát, phản hồi khảo sát và tài khoản. File `.xlsx` được kiểm tra theo từng dòng; bản ghi trùng khóa nghiệp vụ sẽ được cập nhật thay vì tạo thêm. Ngày dùng định dạng `yyyy-MM-dd`.

Riêng màn hình **Đoàn viên** vẫn hỗ trợ nhập/xuất CSV UTF-8 để tương thích với quy trình cũ.

## Nhập tài chính nội bộ từ CSV

Vào **Tích hợp dữ liệu**, xuất CSV tài chính để lấy cấu trúc mẫu gồm `entryCode,unitCode,transactionDate,entryType,category,amount,description,documentNumber,documentStatus`. Ngày dùng `yyyy-MM-dd`; `entryType` nhận `INCOME`/`EXPENSE`; `documentStatus` nhận `COMPLETE`/`INCOMPLETE`/`NOT_REQUIRED`. Mã phiếu đã có sẽ được cập nhật.

## Kiểm thử

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
```

Flyway tạo schema và nạp một bộ dữ liệu demo ở lần chạy đầu. Thay đổi cấu trúc database phải được thêm bằng migration mới trong `backend/src/main/resources/db/migration`.
