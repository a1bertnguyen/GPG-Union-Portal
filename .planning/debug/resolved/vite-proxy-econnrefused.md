---
status: resolved
trigger: "Vite http proxy error: /api/auth/login — AggregateError [ECONNREFUSED]"
created: 2026-08-26
updated: 2026-08-26
---

## Symptoms

- expected: Submitting the login form through Vite reaches the backend `/api/auth/login` endpoint.
- actual: Vite returns an HTTP proxy `ECONNREFUSED` error.
- errors: `AggregateError [ECONNREFUSED] at internalConnectMultiple`.
- timeline: Observed at 03:16 on 2026-08-26; prior working status not provided.
- reproduction: Run the Vite dev server and submit the login form.

## Current Focus

- hypothesis: Confirmed — the running backend binds to 3639 because the local default was changed from 3638, while every frontend/deployment consumer still targets 3638.
- test: Start the corrected backend on 3638 and probe health plus login through the Vite proxy.
- expecting: Backend binds to port 3638; health and login requests no longer produce connection refusal.
- next_action: None; corrected backend is running on 3638 and Vite proxy verification passes.
- reasoning_checkpoint: Backend PID 12860 is healthy on 3639; committed config and all consumers use 3638, proving local port drift.
- tdd_checkpoint: Existing backend tests passed 45/45 in the preceding debug session.

## Evidence

- timestamp: 2026-08-26T03:16:25+07:00
  observation: Vite reports connection refusal for `/api/auth/login`.
  implication: The frontend dev server is alive, but its configured upstream is unreachable.
- timestamp: 2026-08-26T03:20:00+07:00
  observation: `vite.config.ts` maps `/api` to `http://localhost:3638`; no listener exists on 3638 and Docker's API pipe is unavailable.
  implication: The immediate fault is backend availability, not Vite route mapping.
- timestamp: 2026-08-26T03:22:00+07:00
  observation: Spring Boot PID 12860 returns health 200 on port 3639; Git diff shows only the local default changed from 3638 to 3639.
  implication: Correcting the default port restores the established frontend/backend contract without changing authentication code.

## Eliminated

- hypothesis: Vite is proxying login to the wrong configured port.
  reason: The proxy target matches the documented backend port 3638.
- hypothesis: The login controller or authentication service rejects the request.
  reason: Connection refusal occurs before HTTP routing, and the backend health endpoint succeeds on 3639.

## Resolution

- root_cause: The local backend default port had drifted from 3638 to 3639, while Vite, Nginx, Docker Compose, Dockerfile, and documentation all target 3638. The running Spring process was healthy but unreachable through Vite's configured upstream.
- fix: Restored `server.port=${SERVER_PORT:3638}` and restarted only the backend process on the corrected port.
- verification: Port 3638 is owned by the new backend PID 9524; both `/actuator/health` and `/api/auth/login` return HTTP 200 when requested through Vite on port 3637. `AuthSecurityTests` passes 4/4.
- files_changed: `backend/src/main/resources/application.properties`
