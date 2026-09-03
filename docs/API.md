<!-- generated-by: gsd-doc-writer -->
# API

Đăng nhập bằng `POST /api/auth/login`; endpoint nghiệp vụ dùng `Authorization: Bearer <access-token>`. JWT ký bằng HS256. Quyền kỹ thuật gồm `ADMIN`, `USER`; `USER` bị giới hạn theo CĐCS trong token.

| Base path | Chức năng |
|---|---|
| `/api/auth` | Đăng nhập và phiên |
| `/api/dashboard` | Chỉ số điều hành |
| `/api/units`, `/api/members` | CĐCS và đoàn viên |
| `/api/welfare`, `/api/cases` | Chăm lo và kiến nghị |
| `/api/activities`, `/api/finance` | Hoạt động và tài chính |
| `/api/reports`, `/api/kpi` | Báo cáo và KPI |
| `/api/surveys` | Khảo sát |
| `/api/integrations`, `/api/spreadsheets` | CSV và Excel |
| `/api/realtime/events` | SSE |

```http
GET /api/kpi/metadata
GET /api/kpi?periodType=MONTH&year=2026&period=8
GET /api/kpi/evidence/{resourceType}/{recordId}
```

Mã truy cập thường gặp: `401` token không hợp lệ, `403` thiếu quyền, `429` vượt rate limit. Controller tại `backend/src/main/java/vn/gpg/unionportal/controller` là nguồn hợp đồng chính thức.
