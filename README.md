# GPG Union Portal

Hệ thống nghiệp vụ nội bộ dành cho Công đoàn Tổng Công ty và các Công đoàn cơ sở (CĐCS) thuộc Hệ sinh thái Đối Tác Chân Thật – GPG. Ứng dụng quản lý dữ liệu công đoàn tại nơi phát sinh, tổng hợp báo cáo và tính KPI trực tiếp từ hồ sơ nghiệp vụ đã ghi nhận.

## Chức năng chính

- Hồ sơ CĐCS, Ban Chấp hành, nhiệm kỳ, quyết định và đầu mối.
- Hồ sơ người lao động, đoàn viên, biến động và tài liệu liên quan.
- Chính sách và hồ sơ chăm lo: hiếu, hỷ, sinh con, ốm đau, khó khăn, sinh nhật, tri ân và thăm hỏi.
- Kiến nghị, phản ánh, phân công xử lý, SLA, kết quả và chứng từ.
- Kế hoạch chương trình, ngân sách, người tham gia, báo cáo sau hoạt động và thư viện ảnh/tài liệu.
- Thu, chi, tạm ứng, chứng từ và số liệu đối soát tài chính nội bộ.
- Báo cáo định kỳ, dashboard điều hành, khảo sát và tiếng nói người lao động.
- KPI CĐCS theo dữ liệu thật, có trạng thái chất lượng và bằng chứng nguồn.
- Nhập/xuất Excel, tích hợp HR/tài chính bằng CSV và nhật ký lượt nhập.
- Đăng nhập JWT, phân quyền theo vai trò và phạm vi CĐCS.

> Hệ thống tài chính chỉ ghi nhận và tổng hợp dữ liệu nội bộ. Ứng dụng không kết nối ngân hàng, ví điện tử, cổng thanh toán hoặc tự thực hiện giao dịch tiền.

## KPI Công đoàn cơ sở

Màn **Báo cáo KPI** gọi trực tiếp backend; không còn sinh điểm, xếp loại hoặc thứ hạng mô phỏng ở frontend. Một lần đánh giá hỗ trợ bốn loại kỳ:

- `MONTH`: tháng, `period` từ 1 đến 12.
- `QUARTER`: quý, `period` từ 1 đến 4.
- `HALF_YEAR`: 6 tháng, `period` là 1 hoặc 2.
- `YEAR`: năm, `period` là 1.

Engine trả đủ 31 chỉ tiêu thuộc bảy nhóm:

| Nhóm | Nội dung | Trọng số chuẩn |
|---|---|---:|
| `GOV` | Tổ chức, hồ sơ và năng lực BCH | 15 |
| `DATA` | Đoàn viên và chất lượng dữ liệu | 15 |
| `REP` | Báo cáo, kế hoạch và tuân thủ kỳ | 15 |
| `CARE` | Chăm lo, chính sách và quyền lợi NLĐ | 20 |
| `GRV` | Kiến nghị, phản ánh và quan hệ lao động | 15 |
| `ACT` | Hoạt động, chương trình và gắn kết | 10 |
| `FIN` | Tài chính, ngân sách và chứng từ | 10 |

Trọng số, mục tiêu, chiều đánh giá, quy tắc NA và mức phạt thuộc phiên bản cấu hình có ngày hiệu lực. Engine hiện chỉ áp dụng SLA nộp báo cáo khi phiên bản có quy tắc `REPORT_SUBMISSION`; SLA chăm lo, kiến nghị và cập nhật đoàn viên chưa được tính đầy đủ từ lịch làm việc cấu hình. Kết quả kỳ cũ phải tiếp tục gắn với phiên bản đã dùng, không bị thay đổi khi cấu hình mới có hiệu lực.

### Trạng thái chỉ tiêu

- `CALCULATED`: có đủ dữ liệu nguồn để tính tử số, mẫu số và điểm.
- `NA`: không phát sinh hợp lệ, mẫu số bằng 0, KPI cho phép NA và có xác nhận đối soát độc lập.
- `MISSING_DATA`: thiếu dữ liệu hoặc thiếu nguồn xác nhận; không tự động được điểm tối đa.
- `FAILED_VALIDATION`: dữ liệu nguồn có lỗi làm chỉ tiêu không đủ điều kiện tính.

KPI bắt buộc không được chuyển thành NA. Điểm cơ sở phân bổ lại trọng số chỉ cho KPI thật sự NA; dữ liệu thiếu không được dùng để làm tăng điểm. Điểm cuối được giới hạn trong khoảng 0–100 sau thưởng và phạt. Cổng xếp loại có thể hạ mức xếp loại khi thiếu báo cáo bắt buộc, hồ sơ pháp lý/BCH chưa hoàn chỉnh, còn vụ việc nghiêm trọng quá hạn hoặc có vi phạm đã xác minh.

Thưởng/phạt thủ công chỉ được áp dụng khi bản ghi có đủ lý do, người đề nghị, người duyệt, thời điểm duyệt trước ngày chốt và tham chiếu bản ghi minh chứng. Riêng điểm thưởng còn bắt buộc có xác nhận hiệu quả và xác nhận không trùng KPI cơ bản. API trả kèm nhật ký điều chỉnh đã áp dụng; quản trị viên xem đầy đủ, còn người dùng thường thấy mã, số điểm, trạng thái xác minh và thời điểm nhưng phần lý do/danh tính/ID minh chứng được ẩn theo quyền truy cập. Giới hạn thưởng và mức trần phạt vẫn lấy từ phiên bản cấu hình.

Mỗi chi tiết KPI trả về:

- tử số, mẫu số, mục tiêu, trọng số hợp lệ và điểm đạt được;
- giải thích bằng tiếng Việt;
- danh sách module và ID bản ghi dùng trong tử số/mẫu số, kèm liên kết drill-down có kiểm tra phạm vi CĐCS;
- cảnh báo dữ liệu thiếu, quá hạn hoặc không hợp lệ.

Nếu schema nghiệp vụ hiện chưa ghi nhận một sự kiện bắt buộc như thời điểm xác minh, xác nhận NLĐ, biên bản đối soát hay log phê duyệt, engine trả `MISSING_DATA`. Hệ thống không suy đoán từ trường khác và không tạo số liệu để lấp chỗ trống.

### Phạm vi triển khai hiện tại

- `DATA01` được chấm trực tiếp từ hồ sơ đoàn viên đang hoạt động cho kỳ đang diễn ra. Kỳ lịch sử giữ `MISSING_DATA` và số đoàn viên hiển thị là chưa xác định khi chưa có snapshot HR/đoàn viên tại ngày chốt, thay vì dùng trạng thái hiện tại để chấm ngược. `REP01` tự hoạt động khi phiên bản được bổ sung lịch/SLA nộp báo cáo đã phê duyệt.
- `GRV03` chưa cấp điểm từ trường `response_date`, vì workflow hiện gán trường này khi gửi duyệt chứ chưa chứng minh NLĐ đã nhận phản hồi; KPI này giữ `MISSING_DATA` cho đến khi có mốc phản hồi và đóng hồ sơ độc lập.
- Các KPI còn thiếu snapshot HR, nhật ký phê duyệt, mốc xác minh/thanh toán/xác nhận NLĐ hoặc đối soát tài chính được trả về với `MISSING_DATA`; các trường gần giống không được dùng làm số liệu thay thế.
- `dataQualityRate` hiện phản ánh tỷ lệ bản ghi đã nạp vượt qua kiểm tra nhất quán nội bộ. Đối soát độc lập với HR, kế toán và các kênh tiếp nhận vẫn cần được kết nối để trở thành tỷ lệ chất lượng liên nguồn hoàn chỉnh.
- API `GET /api/kpi` là kết quả tạm tính trực tiếp tại thời điểm gọi. Schema đã dành chỗ cho run, detail, evidence và snapshot, nhưng luồng khóa `FINAL`, phê duyệt/mở lại và đọc lại snapshot chưa được công bố thành API; vì vậy kết quả hiện tại không được xem là bảng xếp hạng chính thức.
- Xác nhận `NA` phải thuộc đúng CĐCS, kỳ và phiên bản KPI, đã đối soát bằng một nguồn độc lập khác module nghiệp vụ, có cả người xác nhận lẫn người phê duyệt. Khi chưa có xác nhận hợp lệ, mẫu số 0 vẫn là `MISSING_DATA`.
- Các mã CĐCS từ migration minh họa vẫn được giữ làm danh mục/phạm vi tài khoản, nhưng hồ sơ pháp lý/BCH minh họa và toàn bộ khóa bản ghi mẫu đã được loại khỏi nguồn tính KPI.

### API KPI

```http
GET /api/kpi/metadata
GET /api/kpi?periodType=MONTH&year=2026&period=8
GET /api/kpi?periodType=QUARTER&year=2026&period=3&unitId=1
GET /api/kpi?periodType=HALF_YEAR&year=2026&period=2
GET /api/kpi?periodType=YEAR&year=2026&period=1
GET /api/kpi/evidence/{resourceType}/{recordId}
```

Endpoint metadata cung cấp các phiên bản đã phê duyệt và khoảng ngày hiệu lực. Frontend chỉ cho chọn năm không vượt quá năm hiện tại và có giao với ít nhất một khoảng hiệu lực; các năm nằm trong khoảng trống giữa hai phiên bản không được tự động thêm. Bộ chọn kỳ ẩn kỳ chưa bắt đầu và chỉ nhận kỳ được một phiên bản duy nhất bao phủ toàn bộ ngày bắt đầu–kết thúc. Tài khoản `USER` luôn bị giới hạn về CĐCS trong JWT; truyền `unitId` của đơn vị khác không mở rộng phạm vi. `ADMIN` có thể xem một CĐCS hoặc toàn hệ thống. Frontend dùng nguyên điểm, xếp loại, trạng thái và thứ hạng do engine trả về, chỉ định dạng số hiển thị đến hai chữ số thập phân.

Drill-down chứng cứ chỉ trả một danh sách trường được cho phép và metadata tệp; nội dung tệp tiếp tục đi qua endpoint tải xuống có xác thực. Hồ sơ chăm lo và kiến nghị được ẩn khỏi tài khoản `USER`; `ADMIN` mới nhận liên kết và xem chi tiết nhạy cảm. Xác nhận NA cũng có chứng cứ riêng thể hiện nguồn đối soát, người xác nhận và người duyệt.

## Mô hình sử dụng

- **Đầu mối tuyến đầu:** tạo và cập nhật hồ sơ thuộc phạm vi được giao.
- **BCH/Chủ tịch CĐCS:** quản trị dữ liệu đơn vị, theo dõi KPI tạm tính và gửi/xác nhận báo cáo.
- **Công đoàn Tổng Công ty:** chuẩn hóa danh mục, kiểm tra, yêu cầu bổ sung và tổng hợp toàn hệ thống.
- **Ban Chăm sóc NLĐ:** giám sát hồ sơ chăm lo, kiến nghị và SLA.
- **Kế toán Công đoàn:** kiểm tra giao dịch, chứng từ và đối soát.
- **Ban Lãnh đạo/người phê duyệt:** xem dashboard và phê duyệt theo thẩm quyền.
- **Kiểm toán hệ thống:** xem dữ liệu và nhật ký, không sửa nghiệp vụ.

Ở phiên bản hiện tại, lớp xác thực kỹ thuật dùng hai quyền `ADMIN` và `USER`; các vai trò nghiệp vụ chi tiết ở trên được ánh xạ theo phạm vi công việc. Khi tách thành các quyền riêng, phải giữ nguyên nguyên tắc giới hạn CĐCS và quyền xem hồ sơ nhạy cảm.

Nội dung kiến nghị, dữ liệu sức khỏe và thông tin cá nhân không được đưa lên bảng xếp hạng. API vẫn áp dụng phạm vi đơn vị và quyền truy cập khi cung cấp bằng chứng nguồn.

## Kiến trúc

```text
frontend/   React 19 + TypeScript + Vite
backend/    Java 21 + Spring Boot + Spring Security + Spring Data JPA
database    MySQL 8.4 + Flyway migrations
```

Backend được tổ chức theo các lớp:

- `controller`: REST API và kiểm tra tham số request.
- `service`: nghiệp vụ, phân quyền, KPI và transaction.
- `repository`: truy cập dữ liệu bằng Spring Data JPA.
- `model`: entity và enum miền nghiệp vụ.
- `dto`: hợp đồng request/response.
- `spec`: bộ lọc và truy vấn tổng hợp dùng chung.
- `security`, `config`: JWT, CORS, rate limit và cấu hình ứng dụng.
- `realtime`: sự kiện thay đổi dữ liệu qua Server-Sent Events.

## Chạy bằng Docker

Yêu cầu: Docker Desktop đang hoạt động.

```powershell
Copy-Item .env.example .env
```

Trước khi chạy ngoài máy cá nhân, hãy thay ít nhất `JWT_SECRET`, `ADMIN_PASSWORD` và `USER_PASSWORD` trong `.env`.

```powershell
docker compose up --build
```

Các địa chỉ mặc định:

| Dịch vụ | Địa chỉ |
|---|---|
| Giao diện | http://localhost:3637 |
| Backend API | http://localhost:3638/api |
| Health check | http://localhost:3638/actuator/health |
| MySQL từ máy host | `localhost:3307`, database `union_portal` |

Dừng container nhưng giữ dữ liệu:

```powershell
docker compose down
```

Xóa volume MySQL và khởi tạo lại toàn bộ dữ liệu:

```powershell
docker compose down -v
```

Lệnh cuối là thao tác phá hủy dữ liệu local trong volume Docker.

## Chạy môi trường phát triển

### Backend

Yêu cầu Java 21 và một MySQL có database `union_portal`.

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Có thể ghi đè kết nối bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; cổng mặc định là `3638`. Cấu hình đầy đủ nằm tại `backend/src/main/resources/application.properties`.

### Frontend

Yêu cầu phiên bản Node.js tương thích Vite 8.

```powershell
cd frontend
npm install
npm run dev
```

Frontend dùng `VITE_API_URL`; khi không khai báo, đường dẫn API mặc định là `/api` và Vite proxy request sang backend trong môi trường phát triển.

## Tài khoản khởi tạo local

Khi username chưa tồn tại, backend có thể tạo tài khoản bootstrap từ biến môi trường:

- `ADMIN`: mặc định `admin` / `Admin@123!`.
- `USER`: mặc định `user.vcs` / `User@123!` khi `BOOTSTRAP_USER_ENABLED=true`; tài khoản được gắn với `USER_UNIT_CODE`.

Các thông tin trên chỉ dành cho local/dev. Biến bootstrap không tự đổi mật khẩu của tài khoản đã tồn tại. Không sử dụng mật khẩu hoặc JWT secret mặc định trên môi trường dùng chung.

## Bảo mật và realtime

- API nghiệp vụ dùng JWT HMAC-SHA256; mật khẩu được băm BCrypt.
- Token frontend nằm trong `localStorage` để dùng chung giữa các tab và có thời hạn cấu hình được.
- Backend lấy phạm vi CĐCS của `USER` từ claim `unitId`, không tin phạm vi do client tự gửi.
- Rate limiter áp dụng riêng cho API, đăng nhập và kết nối SSE; response quá hạn trả `429` cùng `Retry-After`.
- Client đã đăng nhập nhận sự kiện thay đổi qua `GET /api/realtime/events`.

```powershell
curl.exe -N -H "Authorization: Bearer <access-token>" http://localhost:3638/api/realtime/events
```

Bộ đếm rate limit hiện nằm trong từng backend instance. Khi chạy nhiều replica cần chuyển trạng thái này sang kho dùng chung, ví dụ Redis, để có hạn mức thống nhất toàn cụm.

## Nhập và xuất dữ liệu

Các module nghiệp vụ hỗ trợ mẫu `.xlsx` và nhập Excel theo từng dòng. Bản ghi trùng khóa nghiệp vụ được cập nhật thay vì tạo bản sao. Ngày sử dụng định dạng `yyyy-MM-dd`.

Tích hợp dữ liệu nội bộ hỗ trợ:

- HR Master bằng CSV, đối chiếu theo mã nhân viên và mã CĐCS.
- Phiếu thu/chi bằng CSV, đối chiếu theo mã phiếu.
- Nhật ký lượt nhập với số dòng thành công, thất bại và chi tiết lỗi.

Nhập hàng loạt và màn tích hợp chỉ dành cho `ADMIN`. KPI luôn đọc lại bản ghi nghiệp vụ; người dùng không nhập một bảng tổng điểm riêng.

Các khóa dữ liệu minh họa từ migration cũ, gồm cả hồ sơ pháp lý/BCH của bốn CĐCS mẫu, được lưu trong danh mục loại trừ của KPI. Engine không dùng các bản ghi này để tính điểm; có thể vô hiệu hóa từng mục loại trừ khi dữ liệu đó đã được xác minh là dữ liệu nghiệp vụ thật.

## Database và migration

Flyway chạy các file tại `backend/src/main/resources/db/migration` theo thứ tự phiên bản. Không sửa migration đã chạy trên môi trường dùng chung; mọi thay đổi schema phải được thêm bằng migration mới. `spring.jpa.hibernate.ddl-auto=validate` giúp phát hiện entity không khớp schema khi khởi động.

Khi thay đổi cấu hình KPI, hãy tạo phiên bản có ngày hiệu lực mới thay vì sửa dữ liệu cấu hình của kỳ đã khóa. Việc tính lại phải dùng cùng snapshot, cutoff và phiên bản nếu cần kết quả tất định.

## Kiểm thử và kiểm tra chất lượng

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Kiểm thử tập trung cho engine KPI và migration V21:

```powershell
.\mvnw.cmd "-Dtest=vn.gpg.unionportal.service.kpi.KpiScoringPolicyTests,vn.gpg.unionportal.service.kpi.GpgKpiEngineTests,vn.gpg.unionportal.service.kpi.KpiMigrationSmokeTests,vn.gpg.unionportal.service.kpi.KpiEvidenceServiceTests" test
```

Frontend:

```powershell
cd frontend
npm test
npm run lint
npm run build
```

`npm run build` gồm TypeScript build, Vite production build và kiểm tra giới hạn bundle. Migration lịch sử `V16__add_activity_program_reports.sql` dùng lệnh `DELIMITER` riêng của MySQL nên bộ integration test khởi tạo toàn bộ schema bằng H2 hiện không chạy được qua V16; đây không phải lỗi của migration KPI V21. Trước khi phát hành cần chạy toàn bộ migration và integration test với MySQL 8.4 đúng phiên bản triển khai.

## Cấu trúc repository

```text
backend/src/main/java/vn/gpg/unionportal/
  config/ controller/ dto/ exception/ i18n/
  mapper/ model/ realtime/ repository/ security/ service/ spec/
backend/src/main/resources/db/migration/
backend/src/test/
frontend/src/
frontend/tests/
docker-compose.yml
.env.example
```

Đặc tả nghiệp vụ KPI là nguồn quyết định cho công thức và quy tắc xếp loại. Khi schema nguồn chưa đủ để chứng minh một KPI, ưu tiên bổ sung sự kiện nghiệp vụ/audit trail trước; không tối ưu điểm bằng cách giảm tiếp nhận kiến nghị, trì hoãn ghi nhận quyền lợi hoặc tạo dữ liệu tổng hợp thủ công.
