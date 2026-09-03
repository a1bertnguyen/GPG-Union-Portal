<!-- generated-by: gsd-doc-writer -->
# Cấu hình

| Biến | Mục đích | Mặc định local |
|---|---|---|
| `DB_URL` | JDBC URL MySQL | database `union_portal` |
| `DB_USERNAME`, `DB_PASSWORD` | Credential database | cấu hình local |
| `SERVER_PORT` | Cổng backend | `3638` |
| `CORS_ALLOWED_ORIGIN` | Origin frontend | `http://localhost:3637` |
| `JWT_SECRET` | Khóa JWT, ít nhất 32 byte | phải thay khi dùng chung |
| `JWT_ACCESS_TOKEN_MINUTES` | Thời hạn token | `480` |
| `BCRYPT_STRENGTH` | Cost BCrypt | `12` |
| `BOOTSTRAP_USER_ENABLED` | Tạo user bootstrap | `false` |
| `RATE_LIMIT_ENABLED` | Bật rate limit | `true` |
| `REALTIME_TIMEOUT_MILLIS` | Timeout SSE | `1800000` |
| `REALTIME_HEARTBEAT_MILLIS` | Heartbeat SSE | `15000` |
| `VITE_API_URL` | Base URL frontend | `/api` |

Nguồn cấu hình: `.env.example`, `docker-compose.yml`, `backend/src/main/resources/application.properties`, `frontend/vite.config.ts`, `vercel.json`.

Không commit `.env`. Rate limiter hiện giữ trạng thái trong từng backend instance; khi chạy nhiều replica cần một store dùng chung.
