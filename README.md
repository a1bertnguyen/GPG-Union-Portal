<!-- generated-by: gsd-doc-writer -->
# GPG Union Portal

> Nền tảng số hóa nghiệp vụ dành cho Công đoàn Tổng Công ty và các Công đoàn cơ sở trong hệ sinh thái GPG.

GPG Union Portal tập trung hồ sơ đoàn viên, phúc lợi, kiến nghị, hoạt động, tài chính, báo cáo và KPI trên cùng một hệ thống. Dữ liệu được ghi nhận tại nơi phát sinh, giới hạn theo phạm vi CĐCS và có thể truy nguyên tới hồ sơ nguồn.

## Điểm nổi bật

- Quản lý CĐCS, Ban Chấp hành, người lao động, đoàn viên và biến động.
- Theo dõi chăm lo, kiến nghị, chương trình, ngân sách và phê duyệt.
- Tính 31 KPI thuộc 7 nhóm từ dữ liệu nghiệp vụ thực tế.
- Nhập/xuất Excel; tích hợp HR và tài chính bằng CSV.
- JWT, quyền `ADMIN`/`USER`, giới hạn dữ liệu theo CĐCS và realtime SSE.

> Phân hệ tài chính chỉ ghi nhận và tổng hợp dữ liệu nội bộ; hệ thống không thực hiện giao dịch tiền.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8, Oxlint |
| Backend | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA |
| Dữ liệu | MySQL 8.4, Flyway |
| Local | Docker Compose, Nginx |

## Khởi động nhanh

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Đổi `JWT_SECRET`, `ADMIN_PASSWORD` và `USER_PASSWORD` trong `.env` trước khi dùng ngoài máy cá nhân.

| Dịch vụ | Địa chỉ |
|---|---|
| Giao diện | <http://localhost:3637> |
| REST API | <http://localhost:3638/api> |
| Health check | <http://localhost:3638/actuator/health> |
| MySQL | `localhost:3307` |

## Kiến trúc

```text
Browser → React/Vite → REST + JWT → Spring Boot → JPA/Flyway → MySQL
                     ↖────── SSE ──────↙
```

Backend theo lớp `controller → service → repository → database`. Frontend tổ chức theo `pages`, `components`, `hooks` và lớp gọi API. Frontend không tự tính điểm KPI; backend là nguồn kết quả.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [Bắt đầu](docs/GETTING-STARTED.md) | Cài đặt và chạy lần đầu |
| [Kiến trúc](docs/ARCHITECTURE.md) | Thành phần và luồng dữ liệu |
| [Phát triển](docs/DEVELOPMENT.md) | Quy trình và lệnh thường dùng |
| [Kiểm thử](docs/TESTING.md) | Test backend/frontend |
| [Cấu hình](docs/CONFIGURATION.md) | Biến môi trường và bảo mật |
| [API](docs/API.md) | Xác thực và nhóm endpoint |
| [Triển khai](docs/DEPLOYMENT.md) | Docker, Vercel, production checklist |
| [Frontend](frontend/README.md) · [Backend](backend/README.md) | Hướng dẫn theo từng phần |

## Kiểm tra chất lượng

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm test
npm run lint
npm run build
```

## Bảo mật và KPI

- Không commit `.env` hoặc dữ liệu thật; JWT secret phải dài tối thiểu 32 byte.
- `USER` chỉ truy cập dữ liệu thuộc `unitId` trong token.
- KPI có trạng thái `CALCULATED`, `NA`, `MISSING_DATA`, `FAILED_VALIDATION`; dữ liệu thiếu không được coi là đạt tối đa.
- `GET /api/kpi` hiện trả kết quả tạm tính tại thời điểm gọi, chưa phải bảng xếp hạng khóa chính thức.

## Giấy phép

Repository chưa khai báo giấy phép. Không mặc định sao chép, phân phối hoặc sử dụng lại mã nguồn ngoài phạm vi được chủ sở hữu cho phép.
