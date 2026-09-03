<!-- generated-by: gsd-doc-writer -->
# Bắt đầu

## Yêu cầu

Nên dùng Docker Desktop. Nếu chạy riêng từng phần: Java 21, Node.js 24, npm và MySQL 8.4.

## Docker

```powershell
git clone <repository-url>
cd CONGDOAN
Copy-Item .env.example .env
docker compose up --build
```

Mở <http://localhost:3637>. API ở <http://localhost:3638/api>.

## Chạy riêng

```powershell
cd backend
.\mvnw.cmd spring-boot:run

cd ..\frontend
npm install
npm run dev
```

## Lỗi thường gặp

- Cổng `3637`, `3638`, `3307` bận: dừng tiến trình cũ hoặc đổi port mapping.
- Lỗi MySQL: kiểm tra `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và container health.
- JWT lỗi: `JWT_SECRET` phải dài tối thiểu 32 byte.
- Frontend mất kết nối: kiểm tra backend cổng `3638` và `VITE_API_URL`.

Đọc tiếp [phát triển](DEVELOPMENT.md), [kiểm thử](TESTING.md), [cấu hình](CONFIGURATION.md).
