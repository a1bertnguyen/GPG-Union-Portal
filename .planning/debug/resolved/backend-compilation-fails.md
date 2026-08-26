---
status: resolved
trigger: "debugg và test lại code hình như code BE bị sai&#x20;"
created: 2026-08-26
updated: 2026-08-26
---

## Symptoms

- expected: `backend\\mvnw.cmd test` compiles the backend and all tests pass.
- actual: Maven stops during `maven-compiler-plugin:compile`; no tests execute.
- errors: `Specs.java:[180,47] no suitable method found for cast(Path<?>, Class<String>)`.
- timeline: Not provided; reproduced against the current dirty working tree on 2026-08-26.
- reproduction: Run `.\\mvnw.cmd test` from `backend`.

## Current Focus

- hypothesis: Confirmed — `Specs.asText` passes the generic JPA `Path<?>` to Hibernate 7's `cast`, which requires a Hibernate `JpaExpression<T>`.
- test: Compile and run the full Maven test suite after casting the Hibernate-backed path to `JpaExpression<?>`.
- expecting: Backend compilation succeeds so the test suite can expose any remaining runtime or behavioral failures.
- next_action: None; compile blocker fixed and the clean backend suite passes.
- reasoning_checkpoint: Hibernate 7.4.5 source confirms `JpaExpression.cast(Class<X>)`; GitNexus impact risk is LOW.
- tdd_checkpoint: Existing test suite is the regression gate.

## Evidence

- timestamp: 2026-08-26T02:51:42+07:00
  observation: Full Maven test command exits 1 during main-source compilation.
  implication: The first blocker is a compile-time Criteria API incompatibility, not an H2 test-data or controller runtime failure.
- timestamp: 2026-08-26T03:00:00+07:00
  observation: `asText` is called by `valueLike`; the traced flow reaches it through member workspace search, and Hibernate 7.4.5 declares `JpaExpression.cast(Class<X>)`.
  implication: A narrow type refinement at the provider boundary is sufficient; no controller/service contract needs to change.

## Eliminated

- hypothesis: Backend tests fail because MySQL is unavailable.
  reason: Compilation fails before Spring test contexts or database connections start.

## Resolution

- root_cause: Hibernate 7.4.5's criteria `cast` API requires `JpaExpression<T>`, but `Specs.asText` supplied a value declared only as Jakarta `Path<?>`, causing Java overload resolution to fail.
- fix: Treat the Hibernate-backed path as `JpaExpression<?>` and call its typed `cast(String.class)` method.
- verification: Both `.\\mvnw.cmd test` and `.\\mvnw.cmd clean test` completed successfully; 45 tests run, 0 failures, 0 errors, 0 skipped.
- files_changed: `backend/src/main/java/vn/gpg/unionportal/spec/Specs.java`
