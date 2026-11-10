# CLAUDE.md

Housekeeping API of StaffApp: campsite managers plan the cleaning of accommodations, employees do it, and the PMS
(property management system) is told when an accommodation is clean. Spring Boot 4, Java 21, PostgreSQL, Flyway, JWT.
The brief is in `candidate-brief.md`, the setup in `README.md`. Decisions go in `NOTES.md` (one page, five sections).

> **Status: this file is a target specification.** The branch only contains the provided project (see "Current
> state"). Everything under "Target" is still to build. Update "Current state" as the work lands, and never describe
> as existing what is not merged yet.

## Commands

```bash
./mvnw clean test                    # all tests (Testcontainers starts PostgreSQL)
./mvnw clean test -Dtest=RoomControllerTest
docker compose up -d postgres        # local PostgreSQL on port 5433
./mvnw spring-boot:run               # API on :8080 (add "-Dspring-boot.run.profiles=dev" for Swagger UI)
```

- Always `clean test` before claiming a result: the incremental compiler has missed changed test files here.
- On this Windows machine `python` is the Microsoft Store stub: it does nothing and reports no error. Never edit files
  through a Python script; use the edit tools or `sed`.

## Current state (provided code, branch `main`)

Packages under `com.staffapp.housekeeping`: `establishment`, `room` (`GET /api/rooms`, `GET /api/rooms/{id}`),
`staff`, `pms` (`PmsClient`, `LoggingPmsClient`), `security` (JWT resource server, HS256), `tenant` (`TenantFilter`,
`CurrentEstablishment`), `dev` (Swagger UI, `/dev/tokens`, dev profile only). Migrations V1 (schema) and V2 (demo
data). One test class, `RoomControllerTest`.

**Known flaws, to fix first** (each with a test that reproduces it before the fix):

| # | Where | Flaw | Rule broken |
|---|-------|------|-------------|
| C1 | `TenantFilter` | `X-ESTABLISHMENT-ID` is trusted as is, never compared with the `establishment_id` claim: any user reads another campsite | R4 |
| C2 | `RoomService.findById` | Looks a room up by id only (IDOR) | R4 |
| C3 | `LoggingPmsClient` | `@Component` in every profile: in production every PMS notification is logged and dropped | R5 |
| H1 | `application.yml` | JWT signing key in the base config: anyone reading the repo signs a MANAGER token | R3, R4 |
| H2 | `RoomService` | `RuntimeException` for a missing room: a 500 instead of a 404, no error format | API |
| H3 | `SecurityConfiguration` | Claims not validated: `role: "STAFF MANAGER"` gives both roles, a token without `role` is authenticated | R3 |

Smaller: `TenantFilter` answers in plain text; `/dev/tokens` mints any token if the dev profile reaches production;
HS256 shared key instead of RS256/JWKS.

## Business rules (the Product Owner's words)

| Rule | Target enforcement |
|------|--------------------|
| **R1** Never two open tasks (`TODO`, `IN_PROGRESS`) on the same room, whatever their day | Partial unique index on `room_id WHERE status IN ('TODO','IN_PROGRESS')`; its violation, recognised by the constraint name, is a 409. No check-then-insert: two managers would race |
| **R2** To do, then in progress, then done. No shortcut, no way back | Exhaustive switch on the status enum, no setter on `status`, a `CHECK` tying `started_at` / `done_at` to the status |
| **R3** Only managers plan (create, assign, reschedule) | URL rules in the security chain, checked before the body is read (not `@PreAuthorize`: it runs after validation and would answer 400 to an employee) |
| **R4** A campsite never sees the tasks, rooms or employees of another one | Header checked against the token (403); every lookup by `id AND establishment_id` (404, never 403); composite FKs `(id, establishment_id)` so a task, its room and its assignee share an establishment |
| **R5** The PMS is never told a room is clean too early, and is always told in the end | Transactional outbox: completing a task, marking the room cleaned and recording the notification happen in one transaction; a relay sends it after the commit, with retries |

Decisions to keep (and to write in `NOTES.md`):
- A task is planned and rescheduled for today or later **in the campsite's day** (one zone, `Europe/Paris`).
- A task starts **on its day or later**, never before (the room may be occupied); a late task can still be done.
- Any employee or manager of the campsite can start or complete a task, assigned to them or not.
- A task in progress can be reassigned, not rescheduled; a done task cannot be changed. No cancellation at first.
- A forgotten task of a past day is rescheduled, never completed without the cleaning being done.

## Target API

All `/api/**` calls need `Authorization: Bearer <JWT>` and `X-ESTABLISHMENT-ID`. Errors are RFC 9457 problem details
(`application/problem+json`); a 400 on a body names the fields in `"errors": {"field": ["message"]}`.

| Endpoint | Who | Success | Errors |
|----------|-----|---------|--------|
| `POST /api/cleaning-tasks` `{roomId, date, assigneeId?, comment?}` (comment ≤ 500) | manager | 201 + `Location` | 400, 403, 404, 409 |
| `GET /api/cleaning-tasks?date=&status=&page=&size=` (a date or a status required, size ≤ 100) | all | 200, page | 400 |
| `GET /api/cleaning-tasks/{id}` | all | 200 | 404 |
| `PATCH /api/cleaning-tasks/{id}` `{assigneeId?, date?}` (absent field = unchanged) | manager | 200 | 400, 403, 404, 409 |
| `POST /api/cleaning-tasks/{id}/start` · `/complete` | all | 200 | 404, 409 |

Status changes are action endpoints, not a `PATCH` of the status. The list is paged, in a stable order (day, room
number, id), with the room and the assignee fetched in the same query: no N+1 ("several hundred tasks a day").

## Target data model (new Flyway versions, never edit a committed one)

- `cleaning_task`: establishment, room, optional assignee, `scheduled_on DATE`, `status`, `comment VARCHAR(500)`,
  `created_by`, `created_at` / `started_at` / `done_at` (`TIMESTAMPTZ`), `version` (optimistic locking). Indexes on
  `(establishment_id, scheduled_on, status)`, `(establishment_id, status)`, and on the FK columns.
- `pms_notification` (outbox): `task_id UNIQUE` (one notification per task), `room_id`, `cleaned_at`, `attempts`,
  `next_attempt_at`, `sent_at`, `last_error`. Partial indexes on what is still to send.

## Target design

- **Entity with behaviour**: `CleaningTask.plan / start / complete / assignTo / reschedule`; `complete` also marks the
  room cleaned. Flush the versioned task update **before** writing the notification, so two simultaneous completions
  give a 409 on the version, not a 500 on the unique notification.
- **PMS outbox relay**, never calling the PMS inside a transaction: (1) a short transaction leases a batch
  (`FOR UPDATE SKIP LOCKED`), (2) the PMS is called with no connection or lock held, (3) a short transaction records
  each result. Keep the order of a room's cleanings (a notification waits for the older ones of its room); a run only
  sends what was due when it started; never send a notification whose lease is about to end; backoff 5 s to 15 min.
  Delivery is at least once ("room cleaned at T" is idempotent for the PMS).
- `LoggingPmsClient` only with `app.pms.client=logging` (dev, tests): without a real client, the app must not start.

## Conventions

- **Time**: inject a `Clock` (the only source of time, in the campsites' zone); `Instant` for moments, `LocalDate` for
  a day, never `LocalDateTime`. No `@FutureOrPresent`: it reads the system clock.
- **Entities** are classes with behaviour and no setters (JPA forbids records); **records** for requests, responses
  and pages. A domain invariant throws `IllegalArgumentException`; input validation answers 400 before reaching it.
- **Errors**: another campsite's resource is a 404 with the same message as a missing one. Map only the database
  constraints we know, by name; never turn every `DataIntegrityViolationException` into a 409.
- **Tests first** (TDD): every rule has a test of the refusal, not only of the happy path. Integration tests that go
  through HTTP are **not** `@Transactional`: they commit like production and clean up in `@AfterEach`. Fix the time
  with `@TestBean Clock`. Test concurrency with a real PostgreSQL lock (`pg_stat_activity`), never with `sleep`. Test
  configuration goes in `src/test/resources/config/application.yml` (a root test `application.yml` would replace the
  main one).
- **Test helpers**: `TestJwtFactory.token(user)`, `DemoData`, `TestcontainersConfiguration`. Demo users: alice, bob
  (STAFF) and marc (MANAGER) in Les Pins (id 1, rooms A01–A10 = ids 1–10); claire (STAFF) and nina (MANAGER) in
  La Plage (id 2, rooms B01–B10 = ids 11–20).
- **Git flow**: `develop` from `main`; one `feature/*` (or `bugfix/*`) branch per topic from `develop`, merged back with
  `--no-ff`; PR to `main` at the end. Commits: gitmoji + type(scope), imperative English subject, body explaining why,
  e.g. `:sparkles: feat(task) : Let managers plan the cleaning of a room`. One topic per commit; each commit builds
  and passes its tests. Never commit or push without being asked.

## Suggested order

1. Fix C1, C2, H2 (isolation and error format), then H1 and C3, each on its own branch with its tests.
2. Task model and migration, then plan (`POST`), start / complete with the outbox, list, assign / reschedule.
3. `NOTES.md`, then the PR. H3 and Swagger annotations if time allows; otherwise list them in "To do".

A complete earlier implementation of this specification exists on the branch `feature/cleaning-tasks`, for reference.
