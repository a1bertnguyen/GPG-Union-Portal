---
phase: full-codebase
reviewed: 2026-08-27T08:58:11Z
depth: deep
scope: repository
files_reviewed: 181
files_reviewed_list:
  - .env.example
  - backend/Dockerfile
  - backend/Dockerfile.vercel
  - backend/pom.xml
  - backend/src/main/java/vn/gpg/unionportal/config/AdminBootstrap.java
  - backend/src/main/java/vn/gpg/unionportal/config/CorsConfig.java
  - backend/src/main/java/vn/gpg/unionportal/config/RateLimitProperties.java
  - backend/src/main/java/vn/gpg/unionportal/config/RealtimeConfig.java
  - backend/src/main/java/vn/gpg/unionportal/config/SecurityConfig.java
  - backend/src/main/java/vn/gpg/unionportal/controller/ActivityController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/ActivityMediaController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/AuthController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/DashboardController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/EngagementController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/FinanceController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/IntegrationController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/LaborCaseController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/MemberController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/MemberWorkspaceController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/MetaController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/PulseSurveyController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/RealtimeController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/ReportController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/SpreadsheetController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/UnionUnitController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/UserAccountController.java
  - backend/src/main/java/vn/gpg/unionportal/controller/WelfareController.java
  - backend/src/main/java/vn/gpg/unionportal/dto/ApiModels.java
  - backend/src/main/java/vn/gpg/unionportal/dto/AuthModels.java
  - backend/src/main/java/vn/gpg/unionportal/dto/ListQuery.java
  - backend/src/main/java/vn/gpg/unionportal/dto/RealtimeEvent.java
  - backend/src/main/java/vn/gpg/unionportal/dto/UserAccountModels.java
  - backend/src/main/java/vn/gpg/unionportal/exception/ApiExceptionHandler.java
  - backend/src/main/java/vn/gpg/unionportal/exception/ResourceNotFoundException.java
  - backend/src/main/java/vn/gpg/unionportal/i18n/EnumLabels.java
  - backend/src/main/java/vn/gpg/unionportal/mapper/EntityMapper.java
  - backend/src/main/java/vn/gpg/unionportal/model/ActivityMedia.java
  - backend/src/main/java/vn/gpg/unionportal/model/AdminUser.java
  - backend/src/main/java/vn/gpg/unionportal/model/BaseEntity.java
  - backend/src/main/java/vn/gpg/unionportal/model/DomainEnums.java
  - backend/src/main/java/vn/gpg/unionportal/model/FinanceEntry.java
  - backend/src/main/java/vn/gpg/unionportal/model/IntegrationRun.java
  - backend/src/main/java/vn/gpg/unionportal/model/LaborCase.java
  - backend/src/main/java/vn/gpg/unionportal/model/Member.java
  - backend/src/main/java/vn/gpg/unionportal/model/MemberChange.java
  - backend/src/main/java/vn/gpg/unionportal/model/MemberDocument.java
  - backend/src/main/java/vn/gpg/unionportal/model/MonthlyReport.java
  - backend/src/main/java/vn/gpg/unionportal/model/PulseSurvey.java
  - backend/src/main/java/vn/gpg/unionportal/model/PulseSurveyResponse.java
  - backend/src/main/java/vn/gpg/unionportal/model/UnionActivity.java
  - backend/src/main/java/vn/gpg/unionportal/model/UnionUnit.java
  - backend/src/main/java/vn/gpg/unionportal/model/WelfareRecord.java
  - backend/src/main/java/vn/gpg/unionportal/realtime/DomainChangeEvent.java
  - backend/src/main/java/vn/gpg/unionportal/repository/ActivityMediaRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/AdminUserRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/FinanceEntryRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/IntegrationRunRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/LaborCaseRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/MemberChangeRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/MemberDocumentRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/MemberRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/MonthlyReportRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/PulseSurveyRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/PulseSurveyResponseRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/UnionActivityRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/UnionUnitRepository.java
  - backend/src/main/java/vn/gpg/unionportal/repository/WelfareRecordRepository.java
  - backend/src/main/java/vn/gpg/unionportal/security/ActiveSessionFilter.java
  - backend/src/main/java/vn/gpg/unionportal/security/RaceSafeRateLimiter.java
  - backend/src/main/java/vn/gpg/unionportal/security/RateLimitFilter.java
  - backend/src/main/java/vn/gpg/unionportal/service/ActivityMediaService.java
  - backend/src/main/java/vn/gpg/unionportal/service/ActivityService.java
  - backend/src/main/java/vn/gpg/unionportal/service/AuthService.java
  - backend/src/main/java/vn/gpg/unionportal/service/CurrentUserService.java
  - backend/src/main/java/vn/gpg/unionportal/service/DataIntegrationService.java
  - backend/src/main/java/vn/gpg/unionportal/service/EngagementService.java
  - backend/src/main/java/vn/gpg/unionportal/service/FinanceService.java
  - backend/src/main/java/vn/gpg/unionportal/service/JwtTokenService.java
  - backend/src/main/java/vn/gpg/unionportal/service/LaborCaseService.java
  - backend/src/main/java/vn/gpg/unionportal/service/MemberCsvService.java
  - backend/src/main/java/vn/gpg/unionportal/service/MemberExcelService.java
  - backend/src/main/java/vn/gpg/unionportal/service/MemberService.java
  - backend/src/main/java/vn/gpg/unionportal/service/MemberWorkspaceService.java
  - backend/src/main/java/vn/gpg/unionportal/service/MonthlyReportService.java
  - backend/src/main/java/vn/gpg/unionportal/service/PulseSurveyService.java
  - backend/src/main/java/vn/gpg/unionportal/service/RealtimeEventPublisher.java
  - backend/src/main/java/vn/gpg/unionportal/service/RealtimeEventService.java
  - backend/src/main/java/vn/gpg/unionportal/service/ReportingService.java
  - backend/src/main/java/vn/gpg/unionportal/service/SpreadsheetImportService.java
  - backend/src/main/java/vn/gpg/unionportal/service/UnionUnitService.java
  - backend/src/main/java/vn/gpg/unionportal/service/UserAccountService.java
  - backend/src/main/java/vn/gpg/unionportal/service/WelfareService.java
  - backend/src/main/java/vn/gpg/unionportal/spec/ActivitySpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/AdminUserSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/FinanceSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/IntegrationRunSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/LaborCaseSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/MemberSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/PulseSurveySpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/SpecAggregates.java
  - backend/src/main/java/vn/gpg/unionportal/spec/Specs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/UnionUnitSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/WelfareSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/spec/WorkspaceSpecs.java
  - backend/src/main/java/vn/gpg/unionportal/UnionPortalApplication.java
  - backend/src/main/resources/application.properties
  - backend/src/main/resources/db/migration/V1__create_core_tables.sql
  - backend/src/main/resources/db/migration/V10__add_dashboard_period_indexes.sql
  - backend/src/main/resources/db/migration/V2__seed_demo_data.sql
  - backend/src/main/resources/db/migration/V3__add_pulse_surveys.sql
  - backend/src/main/resources/db/migration/V4__add_admin_users.sql
  - backend/src/main/resources/db/migration/V5__add_user_unit_scope.sql
  - backend/src/main/resources/db/migration/V6__add_integration_runs.sql
  - backend/src/main/resources/db/migration/V7__add_operational_workspaces.sql
  - backend/src/main/resources/db/migration/V8__add_list_pagination_indexes.sql
  - backend/src/main/resources/db/migration/V9__add_active_token_id.sql
  - backend/src/test/java/vn/gpg/unionportal/AuthSecurityTests.java
  - backend/src/test/java/vn/gpg/unionportal/DashboardMonthBindingTests.java
  - backend/src/test/java/vn/gpg/unionportal/DashboardPerformanceQueryTests.java
  - backend/src/test/java/vn/gpg/unionportal/DataIntegrationServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/EngagementServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/ListPaginationTests.java
  - backend/src/test/java/vn/gpg/unionportal/MemberCsvServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/MemberExcelServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/MultipartUploadControllerTests.java
  - backend/src/test/java/vn/gpg/unionportal/OperationalWorkspaceServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/RateLimitIntegrationTests.java
  - backend/src/test/java/vn/gpg/unionportal/RealtimeIntegrationTests.java
  - backend/src/test/java/vn/gpg/unionportal/ReportingServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/security/RaceSafeRateLimiterTests.java
  - backend/src/test/java/vn/gpg/unionportal/SpreadsheetImportServiceTests.java
  - backend/src/test/java/vn/gpg/unionportal/SpreadsheetOptionalHeaderImportTests.java
  - backend/src/test/java/vn/gpg/unionportal/UnionPortalApplicationTests.java
  - backend/src/test/java/vn/gpg/unionportal/UserAccountServiceTests.java
  - backend/src/test/resources/application.properties
  - backend/vercel-entrypoint.sh
  - docker-compose.yml
  - frontend/Dockerfile
  - frontend/index.html
  - frontend/nginx.conf
  - frontend/package-lock.json
  - frontend/package.json
  - frontend/src/api.ts
  - frontend/src/App.css
  - frontend/src/App.tsx
  - frontend/src/auth.ts
  - frontend/src/canonicalUrl.ts
  - frontend/src/components/CrudPage.tsx
  - frontend/src/components/ExcelImportActions.tsx
  - frontend/src/components/ListCard.tsx
  - frontend/src/components/Pagination.tsx
  - frontend/src/components/sidebar/navigation.ts
  - frontend/src/components/sidebar/Sidebar.tsx
  - frontend/src/components/sidebar/SidebarNavGroup.tsx
  - frontend/src/components/sidebar/SidebarNavigation.tsx
  - frontend/src/components/sidebar/SidebarNavItem.tsx
  - frontend/src/components/TableFilterBar.tsx
  - frontend/src/excel.ts
  - frontend/src/hooks/usePagedList.ts
  - frontend/src/index.css
  - frontend/src/main.tsx
  - frontend/src/pages/ActivityGalleryPage.tsx
  - frontend/src/pages/DashboardPage.tsx
  - frontend/src/pages/EngagementPage.tsx
  - frontend/src/pages/HomeDashboardPage.tsx
  - frontend/src/pages/IntegrationsPage.tsx
  - frontend/src/pages/LoginPage.tsx
  - frontend/src/pages/MemberWorkspacePages.tsx
  - frontend/src/pages/OperationalInsightPages.tsx
  - frontend/src/pages/ReportsPage.tsx
  - frontend/src/pages/UsersPage.tsx
  - frontend/src/PortalApp.tsx
  - frontend/src/portalLoader.ts
  - frontend/src/types.ts
  - frontend/tests/auth.test.mjs
  - frontend/tests/canonicalUrl.test.mjs
  - frontend/tsconfig.app.json
  - frontend/tsconfig.json
  - frontend/tsconfig.node.json
  - frontend/vite.config.ts
  - vercel.json
findings:
  critical: 5
  warning: 10
  info: 0
  total: 15
status: issues_found
---

# Full Codebase Review

**Reviewed:** 2026-08-27T08:58:11Z  
**Depth:** deep  
**Files reviewed:** 181  
**Status:** issues_found

## Summary

The review covered all application Java/TypeScript/CSS, tests, Spring and Flyway resources, Docker/Vercel/nginx/build configuration, and root runtime configuration. Import/call relationships around authentication, account updates, data scoping, reporting, realtime delivery, rate limiting, file storage, and frontend request lifecycles were traced across modules. No source file was changed.

Five release-blocking issues were verified: deployable known credentials, failure to revoke JWTs after security-sensitive account edits, a concurrency hole that can remove the last administrator, a stale-response race that can save one unit's report into another unit, and silent lost updates across mutable entities. Ten additional warnings cover memory/query load, stale UI state, time-zone correctness, multi-instance behavior, migration contamination, search semantics, workflow correctness, query amplification, and missing regression tests.

## Critical Issues

### CR-01: Known fallback secrets create a production-capable default administrator

**Severity:** BLOCKER (Critical)  
**Confidence:** High  
**Files:** `backend/src/main/resources/application.properties:4-6`, `backend/src/main/resources/application.properties:39-49`, `backend/src/main/java/vn/gpg/unionportal/config/AdminBootstrap.java:52-69`, `docker-compose.yml:7-8`, `docker-compose.yml:26-34`

**Triggering scenario:** Start a deployment without explicitly providing `DB_PASSWORD`, `JWT_SECRET`, or `ADMIN_PASSWORD`. The application accepts repository-known fallbacks, and `AdminBootstrap` creates `admin / Admin@123!` when that username does not yet exist.

**Impact:** Anyone who knows the repository can authenticate as the first administrator, sign/verify tokens with the known JWT key, or access the database where the fixed database credentials are exposed. The fallback database password in `application.properties` also appears to be a personal credential and must be treated as compromised.

**Recommended fix:** Remove all secret/password defaults. Bind production secrets from a secret manager and fail startup when absent or equal to a deny-listed development value. Gate bootstrap accounts behind an explicit development/test profile, require a one-time random password in non-development environments, and rotate every credential already committed.

### CR-02: Password, role, and unit changes leave the old JWT fully authorized

**Severity:** BLOCKER (Critical)  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/service/UserAccountService.java:99-123`, `backend/src/main/java/vn/gpg/unionportal/service/UserAccountService.java:144-154`, `backend/src/main/java/vn/gpg/unionportal/security/ActiveSessionFilter.java:32-36`, `backend/src/main/java/vn/gpg/unionportal/service/JwtTokenService.java:32-44`, `backend/src/main/java/vn/gpg/unionportal/service/CurrentUserService.java:11-23`, `backend/src/main/java/vn/gpg/unionportal/service/CurrentUserService.java:33-40`

**Triggering scenario:** An administrator resets a user's password, moves that user to another union unit, or downgrades an administrator to `USER` while the edited account has an active login and its username remains unchanged.

**Impact:** `UserAccountService.update` changes the database fields but never clears/rotates `activeTokenId`. `ActiveSessionFilter` therefore continues accepting the previous token ID, while Spring authorization and `CurrentUserService` continue trusting the old role and `unitId` claims for up to the default eight-hour token lifetime. A compromised token survives a password reset; a downgraded administrator retains admin privileges; a moved user keeps access to the previous unit.

**Recommended fix:** Rotate or clear `activeTokenId` on every password, role, active-state, username, or unit-scope change. Prefer a token/session version checked against the account row and load current role/scope from trusted current state for sensitive authorization. Add HTTP integration tests proving old tokens receive `401` immediately after each security-sensitive edit.

### CR-03: Concurrent updates/deletes can remove every active administrator

**Severity:** BLOCKER (Critical)  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/service/UserAccountService.java:98-115`, `backend/src/main/java/vn/gpg/unionportal/service/UserAccountService.java:127-140`, `backend/src/main/java/vn/gpg/unionportal/repository/AdminUserRepository.java:18`

**Triggering scenario:** With two active administrators, each concurrently downgrades, deactivates, or deletes the other account. Both transactions read `countByRoleAndActiveTrue("ADMIN") == 2` before either transaction commits.

**Impact:** Both precondition checks pass and both writes can commit, leaving zero active administrators and locking the organization out of administration. `@Transactional` at the default isolation level does not serialize the count-and-write invariant.

**Recommended fix:** Enforce the invariant under a database lock or serializable coordination point. For example, pessimistically lock a singleton administration-policy row (or all active-admin rows in a deterministic order) before count and mutation, then re-check inside the same transaction. Add a two-thread integration test that requires exactly one operation to fail.

### CR-04: A stale report request can save unit A's narrative into unit B

**Severity:** BLOCKER (Critical)  
**Confidence:** High  
**File:** `frontend/src/pages/ReportsPage.tsx:21-47`

**Triggering scenario:** Select unit/month A, quickly switch to B, and let B's request finish before the slower A request. The A promise later writes its narrative fields after the selectors already show B. Pressing save posts the current `unitId`/`month` (B) together with A's stale form content.

**Impact:** The race crosses from misleading display into persistent cross-unit data corruption. The delete action at lines 55-61 can likewise act on a stale `summary.narrative` while a newer selection is visible.

**Recommended fix:** Give each load an `AbortController` and a monotonically increasing request generation/key. Only apply a response when its `{month, unitId}` still matches the active selection. Store the loaded key alongside the form, disable save/delete until it matches, and add a component test with deliberately reversed response order.

### CR-05: Mutable records have no optimistic concurrency control and silently lose updates

**Severity:** BLOCKER (Critical)  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/model/BaseEntity.java:13-30`, `backend/src/main/java/vn/gpg/unionportal/service/MemberService.java:90-104`, `backend/src/main/java/vn/gpg/unionportal/service/WelfareService.java:96-104`, `backend/src/main/java/vn/gpg/unionportal/service/LaborCaseService.java:100-108`

**Triggering scenario:** Two users open the same member, welfare record, case, activity, finance entry, report, survey, unit, or account. Both submit edits based on the same old state; the later transaction loads/mutates/saves without any version predicate.

**Impact:** The later commit silently overwrites fields saved by the first user. Timestamps in `BaseEntity` record when the overwrite happened but cannot detect or prevent it, so legitimate business data is lost without warning.

**Recommended fix:** Add a non-null version column by Flyway and `@Version` to the mapped superclass (or each mutable aggregate), expose the version/ETag to clients, and translate optimistic-lock failures to HTTP `409 Conflict`. Add parallel-update integration tests and a client refresh/merge path.

## Warnings

### WR-01: Metadata endpoints eagerly hydrate up to 10 MB BLOBs per row

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/model/MemberDocument.java:36-38`, `backend/src/main/java/vn/gpg/unionportal/model/ActivityMedia.java:39-41`, `backend/src/main/java/vn/gpg/unionportal/service/MemberWorkspaceService.java:113-122`, `backend/src/main/java/vn/gpg/unionportal/service/MemberWorkspaceService.java:147-160`, `backend/src/main/java/vn/gpg/unionportal/service/ActivityMediaService.java:48-53`

**Triggering scenario:** Open a document compliance page or media page containing many maximum-size uploads. Basic fields, including `@Lob byte[]`, are eager by default; the list/compliance DTOs discard `fileData` only after Hibernate has loaded it.

**Impact:** A 20-row media page can materialize roughly 200 MB of payload before serialization. The unbounded compliance search can load substantially more, causing long pauses, GC pressure, database bandwidth spikes, or process OOM from endpoints that return metadata only.

**Recommended fix:** Separate blob content into a dedicated entity/table accessed only by the download query, or use metadata projections that do not select `file_data`. Do not rely on `@Basic(fetch = LAZY)` alone without verifying bytecode enhancement and generated SQL. Add a query-shape test that asserts list/compliance SELECTs omit the blob column.

### WR-02: Several screens and exports still issue unbounded/full-table workloads

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/service/ReportingService.java:110-130`, `backend/src/main/java/vn/gpg/unionportal/service/MonthlyReportService.java:31-36`, `backend/src/main/java/vn/gpg/unionportal/service/DataIntegrationService.java:141-153`, `backend/src/main/java/vn/gpg/unionportal/service/MemberCsvService.java:44-60`, `frontend/src/pages/HomeDashboardPage.tsx:19-27`, `frontend/src/pages/MemberWorkspacePages.tsx:33-42`, `frontend/src/pages/MemberWorkspacePages.tsx:119-128`, `frontend/src/pages/ActivityGalleryPage.tsx:64-67`

**Triggering scenario:** Grow the dataset and open the home dashboard, member workspace, activity gallery, monthly report, finance export, or member CSV export. Multiple paths call repository `findAll()` and filter in Java, while the UI uses `apiAll` to fetch every visible member/activity solely to populate selects or derive cards.

**Impact:** Latency and heap/network consumption grow with the entire organization rather than the requested month/page/unit. This matches the observed multi-second response behavior after rapid navigation: screens can overlap several unbounded requests, even when a paged list component exists elsewhere.

**Recommended fix:** Move month/unit filtering and aggregation into repository specifications/aggregate queries, stream large exports with bounded memory, replace full option lists with scoped autocomplete/lookup endpoints, and cap or authorize `all=true`. Add dataset-scale query-count and latency budgets.

### WR-03: Engagement and case analytics can display an older filter response

**Severity:** WARNING  
**Confidence:** High  
**Files:** `frontend/src/pages/EngagementPage.tsx:54-67`, `frontend/src/pages/OperationalInsightPages.tsx:30-40`

**Triggering scenario:** Change month/unit or case filters several times before prior HTTP requests finish. Neither effect aborts the old request nor verifies which filter produced the response.

**Impact:** A slow old response can overwrite the latest KPI/rollup. Unlike CR-04 these pages do not persist the stale values, but users can make operational decisions from metrics that do not match the visible filters.

**Recommended fix:** Apply the same abort-and-generation pattern already used by `usePagedList`/`DashboardPage`; ignore `AbortError`, and only commit state for the active request key. Add reversed-response-order component tests.

### WR-04: Calendar calculations mix UTC, host time, and Bangkok business time

**Severity:** WARNING  
**Confidence:** High  
**Files:** `frontend/src/api.ts:171`, `frontend/src/pages/HomeDashboardPage.tsx:8-44`, `frontend/src/pages/OperationalInsightPages.tsx:10`, `frontend/src/pages/DashboardPage.tsx:157`, `backend/src/main/java/vn/gpg/unionportal/service/WelfareService.java:55-82`, `backend/src/main/java/vn/gpg/unionportal/service/LaborCaseService.java:58-86`, `backend/src/main/java/vn/gpg/unionportal/service/ReportingService.java:72`, `backend/src/main/resources/application.properties:9-15`, `backend/Dockerfile.vercel:1-14`

**Triggering scenario:** Use the application during the first seven hours of a Bangkok day/month, or deploy the backend on a host whose process time zone is UTC. Frontend `toISOString()` truncation derives the UTC date/month, while backend `LocalDate.now()` uses the JVM default zone. Jackson/Hibernate time-zone properties do not change `LocalDate.now()`.

**Impact:** The default report month can be the previous month; due-today/overdue buckets can be off by one day; the browser and server can disagree on the same record. Docker Compose sets `TZ`, but the Vercel container path does not establish the JVM business zone.

**Recommended fix:** Define the business zone once (`Asia/Bangkok`), inject a `Clock` into backend services, set the JVM zone explicitly in every deployment, and format local calendar dates from local date components or `Intl.DateTimeFormat` rather than UTC ISO strings. Add boundary tests at local midnight and month rollover.

### WR-05: Rate-limit and realtime state are local to one process

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/resources/application.properties:27-36`, `backend/src/main/java/vn/gpg/unionportal/security/RaceSafeRateLimiter.java:10-15`, `backend/src/main/java/vn/gpg/unionportal/service/RealtimeEventService.java:23-55`, `vercel.json:4-20`

**Triggering scenario:** Run more than one backend replica (or allow the container platform to replace/scale instances). A client's rate-limit requests can land on different in-memory token buckets, and an SSE connection on replica A cannot receive a transaction event published on replica B.

**Impact:** The effective rate limit is multiplied by replica count and becomes inconsistent across requests. Realtime clients silently miss committed changes, while sequence IDs reset or diverge per process. Restarting an instance also resets its limiter state.

**Recommended fix:** Use a shared atomic limiter such as Redis and a shared event transport/outbox plus pub/sub. If the product intentionally requires a singleton, enforce and document that deployment constraint and health behavior. Add multi-instance tests; current concurrency/realtime tests exercise one JVM only.

### WR-06: Flyway injects demo business and personal data into every new database

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/resources/application.properties:10-12`, `backend/src/main/resources/db/migration/V2__seed_demo_data.sql:1-43`, `backend/src/main/resources/db/migration/V3__add_pulse_surveys.sql:39-46`

**Triggering scenario:** Provision an empty production database and start the application with Flyway enabled. Versioned migrations unconditionally insert demo units, named members with email/phone data, welfare/case/finance/report records, surveys, and responses.

**Impact:** Production analytics and workflows begin with fabricated records, operators can mistake demo cases for real obligations, and personally styled test data is replicated into every environment. Because these are versioned migrations, profile configuration cannot simply skip only the inserts after deployment.

**Recommended fix:** Keep versioned production migrations schema/reference-data only. Move demo inserts to test fixtures, a separate opt-in dev migration location/profile, or an explicit seed command. Provide a cleanup/verification migration plan for databases where these versions have already run.

### WR-07: Search input `%` and `_` is interpreted as a SQL wildcard

**Severity:** WARNING  
**Confidence:** High  
**File:** `backend/src/main/java/vn/gpg/unionportal/spec/Specs.java:66-70`

**Triggering scenario:** Search for a literal underscore, percent sign, or a string containing either character in any endpoint built on `Specs.textLike`.

**Impact:** `_` matches any single character and `%` matches any sequence, so the result set is much broader than the user's literal query and can degenerate into an expensive near-full scan. Unit scoping remains in place, but search correctness and load controls are defeated.

**Recommended fix:** Escape the escape character, `%`, and `_` before adding surrounding wildcards, then call the Criteria API `like` overload with an explicit escape character. Add literal wildcard tests for every searchable endpoint through the shared specification.

### WR-08: Cancelled welfare work is still classified as due

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/spec/WelfareSpecs.java:31-35`, `backend/src/main/java/vn/gpg/unionportal/service/WelfareService.java:55-78`, `frontend/src/pages/HomeDashboardPage.tsx:9-10`

**Triggering scenario:** Cancel a welfare record whose event date is today, imminent, or overdue, then request due/overdue facets or dashboard metrics.

**Impact:** The backend due predicates exclude only `COMPLETED`, even though the same service's unfinished metric and the frontend's open-record predicate exclude both `COMPLETED` and `CANCELLED`. Cancelled work therefore reappears as actionable and inflates due counts.

**Recommended fix:** Share a single open-work predicate that excludes both terminal statuses and reuse it in specs, facets, dashboard aggregates, and frontend semantics. Add cancelled-record tests for overdue, due-soon, and unfinished metrics.

### WR-09: Pulse survey pages perform one response-count query per survey

**Severity:** WARNING  
**Confidence:** High  
**Files:** `backend/src/main/java/vn/gpg/unionportal/service/PulseSurveyService.java:55-60`, `backend/src/main/java/vn/gpg/unionportal/service/PulseSurveyService.java:149-154`, `backend/src/main/java/vn/gpg/unionportal/repository/PulseSurveyResponseRepository.java:10`

**Triggering scenario:** List a page of surveys or request the unbounded survey list. Each row mapping calls `countBySurveyId` separately.

**Impact:** A 20-row page issues at least 21 queries, and `all=true` grows linearly without a bound. This query amplification compounds the slow-navigation behavior on the engagement screen.

**Recommended fix:** Return counts through a grouped aggregate query/projection (or join to a pre-aggregated subquery) keyed by survey ID, then map all rows from one result set. Add a query-count assertion for page and all modes.

### WR-10: The test suite does not exercise the highest-risk concurrency and UI races

**Severity:** WARNING  
**Confidence:** High  
**Files:** `frontend/package.json:8-10`, `frontend/tests/auth.test.mjs:43-82`, `frontend/tests/canonicalUrl.test.mjs:6-30`, `backend/src/test/java/vn/gpg/unionportal/AuthSecurityTests.java:67-86`, `backend/src/test/java/vn/gpg/unionportal/UserAccountServiceTests.java:46-61`, `backend/src/test/java/vn/gpg/unionportal/RateLimitIntegrationTests.java:32-57`, `backend/src/test/java/vn/gpg/unionportal/RealtimeIntegrationTests.java:39-67`

**Triggering scenario:** Regress any of CR-02 through CR-05 or WR-01/WR-03/WR-05. Existing frontend tests cover only storage/canonical URL helpers; account tests cover newest-login invalidation and a sequential last-admin operation; limiter/SSE integration tests run inside a single process.

**Impact:** Builds remain green while stale tokens retain authority, concurrent admins violate invariants, reversed HTTP responses corrupt form state, concurrent writers lose data, metadata queries hydrate blobs, or replicas disagree. These are exactly the defects that ordinary happy-path service tests will not expose.

**Recommended fix:** Add HTTP security tests for token revocation after password/role/unit changes; two-transaction tests for last-admin and optimistic locking; component tests with controlled/reversed promises for reports, engagement, and analytics; SQL/query-shape tests for BLOB exclusion and N+1 counts; and a two-instance Redis/pub-sub integration test once shared infrastructure is introduced.

## Verification Performed

- Frontend unit tests: 8 passed (`npm test`).
- Frontend lint: passed (`npm run lint`).
- Frontend production build/type-check: passed (`npm run build`).
- Backend targeted non-web suite: 43 tests passed across dashboard binding/query behavior, reporting, engagement, pagination, user accounts, integration/import/export, workspace services, controller binding, application context, and race-safe limiter tests.
- Full backend suite: 50 tests discovered; 43 passed and 7 could not start because this Windows/JDK environment cannot establish the loopback connection required by `HttpClient` and embedded Tomcat. The failures are confined to `AuthSecurityTests`, `RateLimitIntegrationTests`, and `RealtimeIntegrationTests` and occur before application assertions run.
- Frontend dependency audit: `npm audit` reported 0 known vulnerabilities across production and development dependencies.
- GitNexus structural cycle check: no circular dependency findings. Symbol context was used for the account-update/session and realtime call chains; direct source inspection was used where the semantic FTS cache was stale.

Passing tests/builds were treated only as regression evidence, not as proof of correctness. No load test, multi-replica environment, or production MySQL dataset was mutated during this read-only review.

---

_Reviewed: 2026-08-27T08:58:11Z_  
_Reviewer: gsd-code-reviewer_  
_Depth: deep_
