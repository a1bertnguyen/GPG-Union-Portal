<!-- generated-by: gsd-doc-writer -->
# Backend — GPG Union Portal

REST API Java 21/Spring Boot cho nghiệp vụ công đoàn, JWT, KPI, import/export và SSE.

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Backend mặc định chạy tại <http://localhost:3638>; kết nối MySQL được cấu hình qua `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

```text
src/main/java/vn/gpg/unionportal/
├── controller/  REST endpoint
├── service/     Nghiệp vụ, transaction, KPI
├── repository/  Spring Data JPA
├── model/       Entity và enum
├── dto/         Request/response
├── security/    JWT, session, rate limit
└── realtime/    Server-Sent Events
```

Flyway quản lý schema; Hibernate chạy `ddl-auto=validate`. Không sửa migration đã chạy, hãy thêm migration mới. Xem [API](../docs/API.md) và [cấu hình](../docs/CONFIGURATION.md).
