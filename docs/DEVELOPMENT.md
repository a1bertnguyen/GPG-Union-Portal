<!-- generated-by: gsd-doc-writer -->
# Phát triển

```powershell
Copy-Item .env.example .env
docker compose up mysql -d
```

| Khu vực | Lệnh | Mục đích |
|---|---|---|
| Frontend | `npm run dev` | Dev server |
| Frontend | `npm run build` | Type-check, build, audit bundle |
| Frontend | `npm test` | Node tests |
| Frontend | `npm run lint` | Oxlint |
| Backend | `.\mvnw.cmd spring-boot:run` | Chạy API |
| Backend | `.\mvnw.cmd test` | Chạy test |
| Backend | `.\mvnw.cmd package` | Tạo JAR |

## Quy ước

- Không sửa Flyway migration đã chạy; thêm migration mới.
- Đặt nghiệp vụ trong service, giữ controller tập trung vào HTTP.
- API mới phải được phân quyền trong Spring Security.
- Không commit secret, `.env`, dữ liệu cá nhân hoặc hồ sơ thật.
- Thay đổi KPI cần bằng chứng/audit trail, không suy đoán dữ liệu.

Repo chưa tài liệu hóa quy ước nhánh hay PR template. PR nên nêu phạm vi, migration/cấu hình, test đã chạy và ảnh UI nếu có.
