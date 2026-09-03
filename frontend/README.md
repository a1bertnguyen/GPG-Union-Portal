<!-- generated-by: gsd-doc-writer -->
# Frontend — GPG Union Portal

Giao diện React 19 + TypeScript, xây dựng bằng Vite và phục vụ qua Nginx trong container.

```powershell
npm install
npm run dev
```

Vite chạy tại <http://localhost:3637> và proxy `/api`, `/actuator` sang backend cổng `3638`.

| Lệnh | Chức năng |
|---|---|
| `npm run dev` | Chạy dev server |
| `npm run build` | Type-check, build và audit bundle |
| `npm test` | Chạy `tests/*.test.mjs` |
| `npm run lint` | Kiểm tra bằng Oxlint |
| `npm run preview` | Xem production build |

```text
src/
├── components/  Thành phần dùng lại
├── hooks/       Logic React dùng chung
├── pages/       Màn hình nghiệp vụ
├── portal/      Shell và điều hướng
└── assets/      Tài nguyên tĩnh
```

Ứng dụng ưu tiên `VITE_API_URL`; nếu không có, client dùng `/api`. Xem [README chính](../README.md).
