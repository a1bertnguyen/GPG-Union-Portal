<!-- generated-by: gsd-doc-writer -->
# Kiểm thử

## Backend

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=vn.gpg.unionportal.service.kpi.GpgKpiEngineTests" test
```

Test nằm dưới `backend/src/test/java` và dùng hậu tố `Tests.java`.

## Frontend

```powershell
cd frontend
npm test
npm run lint
npm run build
```

Test frontend nằm trong `frontend/tests/*.test.mjs` và chạy bằng Node test runner.

## Lưu ý

Migration `V16__add_activity_program_reports.sql` dùng `DELIMITER` của MySQL, nên trước khi phát hành cần chạy chuỗi migration và integration test trên MySQL 8.4. Repo chưa cấu hình ngưỡng coverage hay workflow CI.
