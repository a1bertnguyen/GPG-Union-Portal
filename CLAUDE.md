# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

GPG Union Portal — digitization platform for a Vietnamese labor union (Công đoàn): member records, welfare (chăm lo), grievances (kiến nghị), activities, finance tracking, monthly reports, and KPI scoring per grassroots union (CĐCS). Domain language, UI strings, API error messages, and docs are Vietnamese — follow that convention.

The finance module only records and aggregates internal data; it never moves money.

## Architecture

```
Browser → React 19 / Vite → REST + Bearer JWT → Spring Boot → JPA/Flyway → MySQL 8.4
```

Modular monolith, layered `controller → service → repository`. Controllers hold HTTP concerns only and are the source of truth for the API surface.

Backend root `backend/src/main/java/vn/gpg/unionportal/`: `controller`, `service` (+ `service/kpi`), `repository` (+ `repository/kpi`), `model` (+ `model/kpi`), `dto`, `mapper`, `spec` (JPA Specifications), `security`, `config`, `i18n`, `realtime`, `validation`, `exception`. Migrations in `src/main/resources/db/migration` (`V{n}__description.sql`, currently through V21).

Frontend root `frontend/src/`: `pages`, `components`, `hooks`, `portal` (portal shell + CRUD config), plus flat modules `api.ts`, `apiCache.ts`, `auth.ts`, `kpiApi.ts`, `kpiModel.ts`, `excel.ts`, `types.ts`.

### Schema is owned by Flyway, not Hibernate

`spring.jpa.hibernate.ddl-auto=validate`. An entity change without a matching migration fails startup instead of auto-creating columns. Never edit a migration that has already run — add a new `V{n}`.

Backend tests run the **real migration chain** against H2 in MySQL mode (`src/test/resources/application.properties`), so a new migration must also parse on H2. `V16__add_activity_program_reports.sql` uses MySQL `DELIMITER` blocks that H2 does not exercise faithfully — run the full chain plus integration tests against real MySQL 8.4 before release.

### KPI engine

`service/kpi/GpgKpiEngine` (~1.2k lines) is the whole scoring pipeline and the only place KPI numbers are produced. The frontend renders what it returns and never computes scores.

Config is data-driven from the `kpi_*` tables seeded by `V21__create_gpg_kpi_engine.sql` (`KpiVersion`, `KpiDefinition`, `KpiClassificationRule`, `KpiClassificationGate`, `PenaltyRule`, `SlaRule`, `BusinessCalendarDay`, `KpiSourceExclusion`, `KpiNoOccurrenceConfirmation`, `KpiAdjustment`) — but the engine cross-checks that data against hardcoded constants: `EXPECTED_CODES` (31 KPI codes), `GROUP_ORDER`/`GROUP_NAMES` (GOV, DATA, REP, CARE, GRV, ACT, FIN), `REQUIRED_PENALTY_CODES` (P01–P07), `REQUIRED_GATE_CODES`, `SUPPORTED_DIRECTIONS`. Config that drifts from those sets makes `evaluate()` throw, so **adding or renaming a KPI needs a new migration _and_ an engine constant change.**

Pipeline per request: resolve period (MONTH/QUARTER/HALF_YEAR/YEAR) → pick the KPI version whose window covers the period start → validate version, catalog, rules, SLA → per unit: load source rows as of a cutoff instant, compute each KPI `Detail`, roll up groups, `baseScore` over eligible weight, add approved bonus (capped by `version.bonusCap`), subtract penalties, classify, let classification gates downgrade the result → rank.

Rules that must not be softened:

- `ResultStatus` is `CALCULATED | NA | MISSING_DATA | FAILED_VALIDATION`. Missing data is never a passing or maximum score; only an approved _and_ reconciled `KpiNoOccurrenceConfirmation` turns a KPI into `NA`.
- Bonus/penalty `KpiAdjustment`s count only with requester, approver, reason, and an `approvedAt` at or before the cutoff.
- `GET /api/kpi` is a live recomputation: `RunStatus` comes back `DRAFT` or `PROVISIONAL` (the latter when data quality is under `version.dataQualityFinalThreshold`), never a locked official ranking.
- Non-admin callers get evidence and adjustment-audit rows flagged `redacted`; sensitive files download only through authenticated endpoints.
- Scores use `BigDecimal` with `MathContext.DECIMAL128`; periods and SLA working-day counts resolve in `Asia/Bangkok` (also pinned for the JVM, JDBC, Jackson, and MySQL).

### Auth, session, authorization

`POST /api/auth/login` → HS256 JWT carrying `roles`, `unitId`, `unitCode` and a random `jti`; `GET /api/auth/me` re-validates it. Roles: `ADMIN`, `USER`.

- **One active session per account.** Login overwrites `AdminUser.activeTokenId`; `ActiveSessionFilter` rejects any request whose `jti` no longer matches with `401 {"code":"SESSION_REPLACED"}`. There is no logout endpoint — the client clears its own storage, and `App.tsx` polls `/auth/me` every 30s to notice a replaced session.
- **`USER` scope comes from the token, never the request.** Route unit filtering through `CurrentUserService.scopedUnitId(requestedUnitId)` (null = ADMIN sees everything) or `requireUnitAccess(unitId)`, and use `Specs.unitScope` / `unitScopeVia` in queries.
- `SecurityConfig` ends in `.anyRequest().hasRole("ADMIN")`, so a new endpoint is ADMIN-only until declared there — including endpoints meant for `USER` screens.
- Permissions are not ordered by seniority: `POST /api/reports` and `PUT /api/reports/*` are `hasRole("USER")` (the CĐCS submits), while `POST /api/reports/*/approve`, `/api/welfare/*/approve`, `/api/cases/*/approve` are ADMIN.
- `RateLimitFilter` + `RaceSafeRateLimiter` are per-instance token buckets tiered login / realtime / default (`app.rate-limit.*`), answering `429`.

### List, error, and label contracts

- Every paginated list binds `dto/ListQuery` with `@ModelAttribute` (`page`, `size` ≤ 200, `all`, `q`, `searchField`, `unitId`, `status`, `preset`, `month`) and returns the envelope `{content, page, size, totalElements, totalPages}`. `all=true` returns everything unpaged, for dropdowns and exports.
- Whole-dataset numbers behind metric cards and status dropdowns come from a sibling `GET {endpoint}/facets` → `{total, statusValues, metrics}`. Filtering and searching always happen server-side in `spec/*Specs`, never in the browser.
- Errors are `{timestamp, code, message}` from `ApiExceptionHandler` (`VALIDATION_ERROR` adds `fields`). Codes the client handles: `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `SESSION_REPLACED`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION_ERROR`, `BAD_REQUEST`, `DATA_CONFLICT`.
- Vietnamese enum labels live once in `i18n/EnumLabels`, are served by `GET /api/meta/enum-labels`, and are matched by `Specs.enumEquals`/search — so search hits the label as well as the constant name. Do not ship a second copy in the frontend.

### Realtime

`RealtimeEventService` holds in-memory `SseEmitter`s, broadcasts `DomainChangeEvent`s after commit (`@TransactionalEventListener(AFTER_COMMIT)` on `realtimeTaskExecutor`), filters per subscriber unit scope, and heartbeats every 15s. Nginx has a dedicated no-buffering location for `/api/realtime/events`.

Note: nothing in `frontend/src` opens an `EventSource` today. The stream is served and unit-scoped, but the React app stays fresh via `apiCache` TTL plus explicit invalidation — treat SSE as a backend capability with no browser consumer yet.

### Frontend patterns

- **No router, no state library.** The only runtime dependencies are `react` and `react-dom`. Navigation is a `PageKey` union in `components/sidebar/navigation.ts` dispatched by a switch in `portal/PortalPage.tsx`; `App.tsx` is the login shell and lazy-loads `PortalApp` through `portalLoader.ts`. A new screen = new `PageKey` + sidebar entry + branch in `PortalPage`.
- Most business screens are `components/CrudPage.tsx` driven by declarative config in `portal/crudFields.ts`, `crudColumns.tsx`, `crudSummaries.ts`. Extend that config before writing a bespoke page.
- `hooks/usePagedList.ts` owns paging, 300ms-debounced search, facets, and a 200ms mount debounce — flicking through tabs deliberately never reaches the network for skipped tabs, because the backend container has little spare capacity.
- `apiCache.ts` is a session-scoped 30s cache with in-flight dedup and refcounted aborts. **After any write, call `invalidateApiCache(pathPrefix)`** or screens keep serving stale rows.
- `api.ts` is the only fetch path: it attaches the bearer token, unwraps `{code, message}` errors, and fires `AUTH_EXPIRED_EVENT` on 401. Use `apiPage`/`apiAll`/`apiFacets` and their `*Cached` variants rather than raw `fetch`.
- Adding a dependency is a real decision: `npm run build` runs `scripts/audit-bundle.mjs`, which fails on published source maps or any `jdbc:`, `DB_PASSWORD`, `JWT_SECRET`, or private-key pattern found in `dist/`.

### API surface

Auth: `POST /api/auth/login`, `GET /api/auth/me`.

| Base path | Purpose |
|---|---|
| `/api/dashboard`, `/api/engagement` | Executive metrics, engagement rollups |
| `/api/units`, `/api/members` | CĐCS and member records |
| `/api/member-changes`, `/api/member-documents` | Member workspace (`MemberWorkspaceController`, mapped at `/api`) |
| `/api/welfare`, `/api/welfare-policies`, `/api/welfare-documents` | Welfare records, policy catalog, attachments |
| `/api/cases`, `/api/case-issue-groups`, `/api/case-documents` | Grievances, issue-group catalog, attachments |
| `/api/activities`, `/api/activity-media` | Programs and media |
| `/api/finance`, `/api/finance-documents` | Finance entries and vouchers |
| `/api/reports` | Monthly and activity reports (submit USER, approve ADMIN) |
| `/api/kpi`, `/api/kpi/metadata`, `/api/kpi/evidence/{resourceType}/{recordId}` | Live KPI dashboard, version windows, evidence drill-down |
| `/api/document-library` | Shared documents (ADMIN writes) |
| `/api/surveys` | Pulse surveys |
| `/api/spreadsheets` | Excel templates, exports, imports |
| `/api/integrations` | CSV finance import/export plus run history |
| `/api/meta/enum-labels` | Vietnamese enum labels |
| `/api/realtime/events` | SSE stream |
| `/actuator/health`, `/actuator/info` | The only public, unauthenticated endpoints |

## Commands

Backend (`backend/`, Java 21, Spring Boot 4.1, Maven wrapper):

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=vn.gpg.unionportal.service.kpi.GpgKpiEngineTests" test   # single class
.\mvnw.cmd "-Dtest=GpgKpiEngineTests#methodName" test                       # single method
.\mvnw.cmd package
```

Tests live in `backend/src/test/java` with the `*Tests.java` suffix; most are `@SpringBootTest` integration tests on H2 plus the full migration chain, so they are slow but do catch schema drift.

Frontend (`frontend/`, React 19, TypeScript 6, Vite 8):

```powershell
npm run dev      # port 3637, proxies /api and /actuator to 3638
npm run build    # tsc -b && vite build && node scripts/audit-bundle.mjs
npm test         # node --test tests/*.test.mjs
npm run lint     # oxlint
```

Full stack:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Frontend `http://localhost:3637`, API `http://localhost:3638/api`, MySQL `localhost:3307`. Vercel deploys both services from `vercel.json` (backend as a container via `backend/Dockerfile.vercel`, with `/api/*` and `/actuator/*` rewritten to it).

## Conventions

- Every backend setting in `application.properties` has a working local default — including credentials and `JWT_SECRET` — which is why the app boots with no `.env`. Override all of them outside local development; a `JWT_SECRET` under 32 bytes fails startup by design.
- Business logic in services, controllers HTTP-only, and authorization for every new endpoint declared in `SecurityConfig`.
- Never commit `.env`, secrets, or real member data. `backend/mock-data/full-demo-data.sql` is the demo seed.
- No CI workflow and no coverage threshold exist — verification is whatever you run locally.
- `AGENTS.md` currently holds only the generated GitNexus block; project instructions live here.

## Documentation

`docs/` has `GETTING-STARTED.md`, `ARCHITECTURE.md`, `DEVELOPMENT.md`, `TESTING.md`, `CONFIGURATION.md`, `API.md`, `DEPLOYMENT.md`; see also `frontend/README.md`, `backend/README.md`, and `REQUIREMENTS-COVERAGE.md` at the root. These are generated Vietnamese summaries — keep them in step when you change behavior they describe.


<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **CONGDOAN** (3326 symbols, 10867 relationships, 274 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## When Debugging

1. `gitnexus_query({query: "<error or symptom>"})` — find execution flows related to the issue
2. `gitnexus_context({name: "<suspect function>"})` — see all callers, callees, and process participation
3. `READ gitnexus://repo/CONGDOAN/process/{processName}` — trace the full execution flow step by step
4. For regressions: `gitnexus_detect_changes({scope: "compare", base_ref: "main"})` — see what your branch changed

## When Refactoring

- **Renaming**: MUST use `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` first. Review the preview — graph edits are safe, text_search edits need manual review. Then run with `dry_run: false`.
- **Extracting/Splitting**: MUST run `gitnexus_context({name: "target"})` to see all incoming/outgoing refs, then `gitnexus_impact({target: "target", direction: "upstream"})` to find all external callers before moving code.
- After any refactor: run `gitnexus_detect_changes({scope: "all"})` to verify only expected files changed.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Tools Quick Reference

| Tool | When to use | Command |
|------|-------------|---------|
| `query` | Find code by concept | `gitnexus_query({query: "auth validation"})` |
| `context` | 360-degree view of one symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius before editing | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Pre-commit scope check | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Safe multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` |
| `cypher` | Custom graph queries | `gitnexus_cypher({query: "MATCH ..."})` |

## Impact Risk Levels

| Depth | Meaning | Action |
|-------|---------|--------|
| d=1 | WILL BREAK — direct callers/importers | MUST update these |
| d=2 | LIKELY AFFECTED — indirect deps | Should test |
| d=3 | MAY NEED TESTING — transitive | Test if critical path |

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/CONGDOAN/context` | Codebase overview, check index freshness |
| `gitnexus://repo/CONGDOAN/clusters` | All functional areas |
| `gitnexus://repo/CONGDOAN/processes` | All execution flows |
| `gitnexus://repo/CONGDOAN/process/{name}` | Step-by-step execution trace |

## Self-Check Before Finishing

Before completing any code modification task, verify:
1. `gitnexus_impact` was run for all modified symbols
2. No HIGH/CRITICAL risk warnings were ignored
3. `gitnexus_detect_changes()` confirms changes match expected scope
4. All d=1 (WILL BREAK) dependents were updated

## Keeping the Index Fresh

After committing code changes, the GitNexus index becomes stale. Re-run analyze to update it:

```bash
npx gitnexus analyze
```

If the index previously included embeddings, preserve them by adding `--embeddings`:

```bash
npx gitnexus analyze --embeddings
```

To check whether embeddings exist, inspect `.gitnexus/meta.json` — the `stats.embeddings` field shows the count (0 means no embeddings). **Running analyze without `--embeddings` will delete any previously generated embeddings.**

> Claude Code users: A PostToolUse hook handles this automatically after `git commit` and `git merge`.

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
