<!-- generated-by: gsd-doc-writer -->
# Triển khai

## Docker Compose

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

Compose chạy MySQL 8.4, backend Spring Boot và frontend Nginx. `docker compose down` giữ volume; `docker compose down -v` xóa dữ liệu local.

## Vercel

`vercel.json` khai báo frontend Vite, backend container từ `backend/Dockerfile.vercel`, vùng `sin1`, và rewrite `/api`, `/actuator` tới backend.

## Checklist production

- Đặt JWT secret tối thiểu 32 byte và credential riêng.
- Dùng database có sao lưu; giới hạn đúng `CORS_ALLOWED_ORIGIN`.
- Xác minh Flyway và `/actuator/health` trước khi nhận traffic.
- Chạy test backend, test/lint/build frontend.
- Nếu có nhiều replica, dùng rate-limit store chung.
