# Kế hoạch triển khai: ghi nhận hoạt động, thống kê và chốt KPI CĐCS (GPG-CD-KPI-V3)

Trạng thái: **Đã triển khai trong working tree, chưa commit**.

## Cập nhật thực thi

- Đã hoàn tất backend: migration V23, catalog V3 đủ 7 nhóm GOV/DATA/REP/CARE/GRV/ACT/FIN,
  snapshot danh sách nhân sự cuối năm, thống kê hoạt động theo năm/tháng/nhóm kiến nghị,
  API lịch sử/chốt kỳ/chứng cứ và phân quyền ADMIN/USER.
- Đã hoàn tất công thức CARE05: sinh nhật tính theo **người lao động duy nhất đã liên kết và hoàn tất** /
  nhân sự cuối năm được duyệt; năm nhóm chăm lo còn lại tính theo **số sự việc**; mẫu số CARE05 là
  nhân sự cuối năm + các nghĩa vụ chăm lo khác.
- Đã thêm liên kết sinh nhật thủ công, lý do hủy bắt buộc, snapshot bất biến khi chốt và cảnh báo đối soát.
- Đã tích hợp màn hình “Thống kê hoạt động”, “Lịch sử đã chốt”, “Đối soát nhân sự” vào trang KPI.
- Xác minh: backend compile và nhóm test KPI/migration hiện có đạt; frontend build/lint/test đạt.
  Full backend suite vẫn còn các lỗi loopback HTTP và một số test import/list đã có từ thay đổi trước đó.

Phần bên dưới là kế hoạch gốc và các ràng buộc thiết kế dùng để đối chiếu khi bảo trì tiếp.

```
[ERROR] .../service/kpi/KpiRunService.java:[147,6] reached end of file while parsing
```

Mục tiêu của kế hoạch: đưa nhánh dở dang này về trạng thái biên dịch được, chốt kỳ được, đọc lịch sử được,
và có giao diện dùng thật — mà không nới lỏng bất kỳ ràng buộc nào trong `CLAUDE.md`.

---

## 1. Những gì đã có trong working tree

Tất cả đều **chưa commit**. Đây là nền của kế hoạch, không phải việc phải làm lại.

| Thành phần | Trạng thái |
|---|---|
| `V22__add_gpg_kpi_v2_catalog.sql` | Xong. Chuyển V1 sang `SUPERSEDED`, thêm `welfare_records.completed_at`, seed V2: 23 chỉ tiêu tổng trọng số 100, 5 mức xếp loại, 4 cổng hạ loại, 7 luật phạt P01–P07, 10 luật SLA (có `REPORT_SUBMISSION` 5 ngày làm việc), 13 ngày lễ. |
| `GpgKpiEngine` | Viết lại, 1506 dòng. `EXPECTED_CODES` đã là 23 mã V2. Đã có sẵn helper cho việc chốt kỳ: `lockEligible`, `asFinal`, `rankingComparator`, `rankOfficial`, `copyRank`. `Summary.lockEligibleCount` đã được điền tại dòng 1240. |
| `KpiSourceEvidenceIndex` | Xong. Kiểm tra theo lô xem bản ghi có tệp đính kèm thật hay không, phục vụ CARE03 / ACT03 / FIN01. Ba truy vấn repository mới đã có. |
| Entity + repository cho `kpi_runs`, `kpi_result_details`, `kpi_evidence`, `kpi_run_warnings` | Xong. Bốn bảng này **đã tồn tại từ V21**, không cần migration mới. |
| DTO `LockResult`, `LockedUnit`, `HistoryPoint`, `UnitHistory`, `History` | Xong trong `KpiModels`. |
| `WelfareRecord.completedAt` + `WelfareService` | Xong. Mốc hoàn tất thật, thay cho `updated_at`. |
| Test | `KpiMigrationSmokeTests.v22ReplacesTheActiveCatalogWithTwentyThreeComputableCodes` đã có. `GpgKpiEngineTests` đã kiểm `lockEligibleCount`. |

## 2. Những gì đang thiếu

1. `KpiRunService.java` bị cắt giữa file ở dòng 147. Thiếu `storeDetails`, `storeWarnings`, `inputHash`, và toàn bộ phần đọc lịch sử mà javadoc của chính class đã hứa.
2. `GpgKpiEngine.BUSINESS_ZONE` đang là `private` (dòng 32) nhưng `KpiRunService` gọi `GpgKpiEngine.BUSINESS_ZONE`. Đây là lỗi biên dịch thứ hai, sẽ hiện ra ngay sau khi hết lỗi parse.
3. Chứng cứ chưa bao giờ được ghi. `KpiEvidenceRowRepository` đã inject nhưng không dùng.
4. Chưa có endpoint: `POST /api/kpi/lock`, `GET /api/kpi/history`.
5. Chưa khai trong `SecurityConfig`. Cả hai sẽ rơi vào `.anyRequest().hasRole("ADMIN")` — đúng cho lock, **sai cho history** vì USER phải xem được lịch sử CĐCS của mình.
6. `KpiService` chưa có đường dẫn qua cho lock/history.
7. Frontend chưa có gì: `KpiDashboardSummary` thiếu `lockEligibleCount`, không có nút chốt kỳ, không có màn hình lịch sử.

---

## Giai đoạn 1 — Gỡ chặn biên dịch

Không làm gì khác trước khi giai đoạn này xong. `mvnw compile` hiện dừng ở lỗi parse nên **chưa ai biết còn bao nhiêu lỗi phía sau**.

### 1.1 `GpgKpiEngine.java` dòng 32

Đổi `private static final ZoneId BUSINESS_ZONE` thành `static final ZoneId BUSINESS_ZONE` (package-private, cùng package `service.kpi`). Không mở `public` — vùng giờ nghiệp vụ không phải API của engine.

### 1.2 `KpiRunService.java` — viết nốt ba method còn thiếu

**`private String inputHash(UnitResult result, Dashboard dashboard, KpiVersion version)`**

SHA-256 hex trên một chuỗi dựng theo thứ tự xác định. Import đã có sẵn: `MessageDigest`, `StandardCharsets`, `HexFormat`.

Đưa vào hash: `version.getVersionId()`, `periodType`, `periodStart`, `periodEnd`, `unionUnitId`; rồi từng `Detail` đã sort theo `kpiCode` với `numerator`, `denominator`, `targetValue`, `resultStatus`; rồi id các bản ghi chứng cứ đã sort; rồi id các `AdjustmentAudit` đã duyệt.

> **Bẫy phải tránh:** `cutoffAt` là `Instant.now()` (engine dòng 130) và `generatedAt` cũng vậy. Nếu hash chứa một trong hai thì mỗi lần chốt lại đều sinh revision mới và nhánh `unchanged` trong `persist` không bao giờ chạy. **Loại cả hai khỏi hash.**

Chuẩn hoá `BigDecimal` bằng `stripTrailingZeros().toPlainString()`, `null` thành một token cố định — nếu không, `1.0` và `1.00` cho ra hai hash khác nhau.

**`private void storeDetails(Long runId, UnitResult result)`**

Với mỗi `Detail` trong `result.details()`: dựng `KpiResultDetail`, `save`, lấy id sinh ra, rồi ghi `KpiEvidenceRow` cho từng phần tử `detail.evidence()` với `resultId` = id vừa lấy. Ràng buộc `fk_kpi_evidence_result` bắt buộc thứ tự này — không thể ghi chứng cứ trước.

Ánh xạ `Detail` → `KpiResultDetail` là một-một trừ `resultId` (String tổng hợp ở chế độ live, bỏ đi khi lưu). Ánh xạ `Evidence` → `KpiEvidenceRow` khớp toàn bộ cột.

Dùng `saveAll` theo lô cho chứng cứ. Một đơn vị lớn sinh rất nhiều dòng.

**`private void storeWarnings(Long runId, UnitResult result)`**

`Warning` → `KpiRunWarning`: `code`→`warningCode`, `severity.name()`→`severity`, cộng `message`, `recommendedAction`, `sourceModule`, `sourceRecordId`, `redacted`.

> `kpi_run_warnings` **không có cột `due_at`**, nên `Warning.dueAt` bị mất khi chốt kỳ. Nếu hạn xử lý cần đọc lại được từ lịch sử thì phải có migration V23 thêm cột. Quyết định này nên chốt trước khi có bản chốt kỳ thật đầu tiên, vì sau đó dữ liệu cũ không tái tạo được.

**Tiêu chí xong:** `./mvnw -q compile` sạch. Xoá các import không dùng còn lại.

---

## Giai đoạn 2 — Đọc lịch sử đã chốt

`KpiRunService` đã inject `UnionUnitRepository`, `CurrentUserService`, `KpiResultDetailRepository` và import `Specs`, `LinkedHashMap`, `Collectors`, `Comparator`, `Function` nhưng chưa dùng. Đó là phần đọc còn thiếu.

**`public History history(PeriodType periodType, int fromYear, int toYear, Long requestedUnitId)`**

1. Phạm vi đơn vị: `Long scoped = currentUser.scopedUnitId(requestedUnitId)`. `null` nghĩa là ADMIN xem tất cả.
2. `KpiRun.unionUnitId` là cột `Long` thuần, **không phải `@ManyToOne`**. `Specs.unitScope` điều hướng `unionUnit.id` nên **không dùng được ở đây** — phải viết predicate riêng `cb.equal(root.get("unionUnitId"), scoped)`. `KpiRunRepository` đã `extends JpaSpecificationExecutor<KpiRun>` cho việc này.
3. Lọc theo `periodType` và `periodStart` trong khoảng `[fromYear-01-01, toYear-12-31]`.
4. Với mỗi (đơn vị, kỳ) chỉ lấy **revision cao nhất**. Các revision cũ vẫn nằm trong bảng để lưu vết, nhưng lịch sử chỉ hiển thị bản mới nhất.
5. Nhóm theo đơn vị thành `UnitHistory`, sắp `HistoryPoint` theo `periodStart` tăng dần.
6. `periodLabel` dựng lại bằng `GpgKpiEngine.resolvePeriod` (đã `public static`) để nhãn kỳ khớp hệt màn hình live.

Chỉ đọc `kpi_runs`; không nạp `kpi_result_details` cho lịch sử tổng hợp. Chi tiết theo từng KPI là một lần đọc riêng theo `runId` khi người dùng bấm vào một kỳ.

**Tiêu chí xong:** USER chỉ thấy đúng CĐCS của mình, kể cả khi truyền `unitId` của đơn vị khác.

---

## Giai đoạn 3 — API và phân quyền

### 3.1 `KpiService` — thêm hai đường dẫn qua

Giữ nguyên quy ước: controller không gọi `KpiRunService` trực tiếp, đi qua `KpiService` như `evaluate`/`metadata` đang làm.

### 3.2 `KpiController`

```
POST /api/kpi/lock?periodType=&year=&period=    → LockResult
GET  /api/kpi/history?periodType=&fromYear=&toYear=&unitId=  → History
```

`fromYear`/`toYear` mặc định về năm hiện tại theo `Asia/Bangkok` khi thiếu, giống cách `defaultPeriod` đang xử lý.

### 3.3 `SecurityConfig` — **bắt buộc**

`.anyRequest().hasRole("ADMIN")` ở cuối chuỗi khiến endpoint mới mặc định là ADMIN-only. Đúng cho lock, sai cho history.

- Thêm `"/api/kpi/history"` vào danh sách `requestMatchers(HttpMethod.GET, ...)` ở dòng ~65 (`hasAnyRole("ADMIN","USER")`).
- Khai `requestMatchers(HttpMethod.POST, "/api/kpi/lock").hasRole("ADMIN")` **tường minh**, đừng dựa vào `anyRequest`.

`KpiRunService.lock` đã tự kiểm `currentUser.isAdmin()` và ném `AccessDeniedException`. Giữ cả hai lớp — kiểm tra ở service là lớp phòng thủ nếu ai đó sửa `SecurityConfig`.

### 3.4 Vô hiệu hoá cache sau khi chốt

Chốt kỳ là một lần ghi. Frontend phải gọi `invalidateApiCache('/kpi')` sau khi `POST /api/kpi/lock` trả về, nếu không màn hình vẫn hiển thị số cũ trong 30 giây.

---

## Giai đoạn 4 — Frontend

### 4.1 `kpiModel.ts`

- `KpiDashboardSummary`: thêm `lockEligibleCount: number`.
- Thêm interface cho `LockResult`, `LockedUnit`, `HistoryPoint`, `UnitHistory`, `History` khớp record backend.

### 4.2 `kpiApi.ts`

Thêm `lockKpiPeriod(params)` và `loadKpiHistory(params)`. Đi qua `api()` như mọi hàm khác — không `fetch` trực tiếp.

### 4.3 `KpiPage.tsx` (584 dòng)

- Thẻ metric thứ năm: "Có thể chốt chính thức" đọc `summary.lockEligibleCount`, đặt cạnh bốn thẻ hiện có ở dòng 448–460.
- Nút "Chốt kỳ" **chỉ hiện với ADMIN**, và chỉ bật khi kỳ đã kết thúc — `lock` sẽ ném `IllegalArgumentException("Chỉ được chốt kỳ đã kết thúc")` nếu không. Vô hiệu nút thay vì để người dùng nhận lỗi.
- Hộp xác nhận trước khi chốt, hiển thị số đơn vị sẽ thành FINAL và số đơn vị bị chặn kèm `blockingKpiCodes`.
- Màn hình lịch sử: biểu đồ điểm theo kỳ cho từng đơn vị. Đây là giá trị thật của việc chốt kỳ — số live không so sánh được giữa các kỳ.

Trang mới cần: thêm `PageKey` trong `components/sidebar/navigation.ts`, mục sidebar, và một nhánh trong `portal/PortalPage.tsx`. Nếu chỉ là tab trong `KpiPage` thì không cần.

Không thêm dependency. `package.json` chỉ có `react` và `react-dom`; `scripts/audit-bundle.mjs` chạy trong `npm run build`.

---

## Giai đoạn 5 — Kiểm thử

### 5.1 Test mới: `KpiRunServiceTests`

| Trường hợp | Kỳ vọng |
|---|---|
| Chốt kỳ đã kết thúc, đơn vị dữ liệu đủ | Ghi `kpi_runs` với `run_status = FINAL`, có `ranking_position` |
| Chốt kỳ đã kết thúc, đơn vị còn `MISSING_DATA` | Vẫn ghi run nhưng `PROVISIONAL`, `blockingKpiCodes` nêu đúng mã |
| Chốt lại khi dữ liệu nguồn không đổi | `unchanged = true`, **không** sinh revision mới, `kpi_runs` không thêm dòng |
| Chốt lại sau khi sửa một bản ghi nguồn | `revision = 2`, `previous_run_id` trỏ về revision 1 |
| Chốt kỳ chưa kết thúc | `IllegalArgumentException` |
| USER gọi lock | `AccessDeniedException` |
| USER đọc history với `unitId` đơn vị khác | Chỉ trả về đơn vị của chính mình |
| Chi tiết đã lưu | `kpi_result_details` có đủ 23 mã, `kpi_evidence` có dòng trỏ đúng `result_id` |

Trường hợp "chốt lại khi không đổi" là bài test quan trọng nhất — nó là bài duy nhất phát hiện lỗi đưa `cutoffAt` vào `inputHash`.

### 5.2 Điểm mù đã biết trong test hiện có

`KpiMigrationSmokeTests` tự tạo `union_units` và `welfare_records` bằng tay rồi mới chạy Flyway trên hai file V21/V22. Cách này **không phát hiện lệch với chuỗi migration thật**. Test `@SpringBootTest` chạy toàn chuỗi trên H2 mới là nơi bắt lệch schema.

`V16__add_activity_program_reports.sql` dùng khối `DELIMITER` của MySQL mà H2 không mô phỏng trung thực. Trước khi phát hành: chạy toàn chuỗi V1→V22 trên **MySQL 8.4 thật**, không chỉ H2.

### 5.3 Lệnh xác minh

```powershell
cd backend
.\mvnw.cmd -q compile                                                    # Giai đoạn 1
.\mvnw.cmd "-Dtest=vn.gpg.unionportal.service.kpi.*Tests" test           # Giai đoạn 1–2
.\mvnw.cmd test                                                          # trước khi commit
cd ..\frontend
npm run build                                                            # tsc -b + audit-bundle
npm run lint
```

---

## Ràng buộc không được nới lỏng

Lấy từ `CLAUDE.md`. Vi phạm bất kỳ dòng nào dưới đây làm sai số KPI theo cách khó phát hiện.

- **`GET /api/kpi` không bao giờ ghi.** Chỉ `lock` được ghi `kpi_runs`. Đọc live luôn trả `DRAFT` hoặc `PROVISIONAL`.
- **Thiếu dữ liệu không bao giờ là điểm tối đa.** Chỉ `KpiNoOccurrenceConfirmation` đã duyệt **và** đã đối soát mới chuyển một KPI sang `NA`.
- **Đơn vị còn `MISSING_DATA` hoặc `FAILED_VALIDATION` không bao giờ thành `FINAL`**, dù điểm tạm tính cao đến đâu. `lockEligible` đã cài đúng — đừng lách qua.
- **Bonus/penalty phải đủ** requester, approver, reason và `approvedAt` không muộn hơn cutoff.
- **Non-admin nhận dòng chứng cứ và audit có cờ `redacted`.** `KpiEvidenceRow.redacted` phải được lưu đúng lúc chốt, không tính lại lúc đọc.
- `BigDecimal` với `MathContext.DECIMAL128`; kỳ và ngày làm việc SLA giải theo `Asia/Bangkok`.
- Thêm hay đổi tên một KPI cần **cả** migration mới **và** sửa hằng số trong engine. Config lệch khỏi `EXPECTED_CODES` làm `evaluate()` ném lỗi.
- Không sửa migration đã chạy. Thêm `V23`.

## Việc cần quyết định trước khi có bản chốt kỳ thật đầu tiên

1. **`due_at` cho `kpi_run_warnings`** — thêm cột ở V23 hay chấp nhận mất hạn xử lý trong lịch sử.
2. **Ngày lễ sau 2027** — `business_calendar_days` chỉ khai đến 2027. Thiếu dòng nào thì ngày đó bị tính là ngày làm việc và hạn SLA sớm hơn thực tế. Cần một màn hình quản trị hoặc quy trình bổ sung hằng năm, kèm Giỗ Tổ Hùng Vương và các ngày nghỉ bù theo thông báo Chính phủ.
3. **Mở lại kỳ đã chốt** — `RunStatus` đã có `REOPENED` nhưng chưa có luồng nào dùng. Nếu cần sửa số sau khi chốt thì phải thiết kế; hiện tại chốt lại chỉ tạo revision mới.
