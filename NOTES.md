# Notes

## 1. Assumptions

- **Open task** = `TODO` or `IN_PROGRESS`, whatever its day: "never two open tasks on the same accommodation" is read
  literally. It also protects the PMS: with two open tasks on a room, completing tomorrow's would report the room clean
  while today's is not done. A task must never be completed without the cleaning being done, even to free its room.
- Any employee or manager of the campsite can start or complete any of its tasks, assigned to them or not.
- A task in progress can be handed over to another employee; a done task cannot be changed. No cancellation, no
  rescheduling and no rule on the day yet (see To do).
- The task list requires a day or a status: teams look at a day or at what is left, not at the whole history.

## 2. API

Every call needs `Authorization: Bearer <JWT>` and `X-ESTABLISHMENT-ID`, which must match the token (else 403).
Errors are RFC 9457 problem details. A resource of another campsite is a **404**, never a 403: ids reveal nothing.

| Endpoint | Who | Success | Errors |
|----------|-----|---------|--------|
| `POST /api/cleaning-tasks` `{roomId, date, assigneeId?, comment?}` | manager | 201 + `Location` | 400, 403, 404, 409 |
| `GET /api/cleaning-tasks?date=&status=&page=&size=` (size ≤ 100) | all | 200, page | 400 |
| `GET /api/cleaning-tasks/{id}` | all | 200 | 404 |
| `PATCH /api/cleaning-tasks/{id}` `{assigneeId}` | manager | 200 | 400, 403, 404, 409 |
| `POST /api/cleaning-tasks/{id}/start` · `/complete` | all | 200 | 404, 409 |

## 3. Technical choices

- **Fixes in the existing code**: the establishment header was trusted as is (any user could read another campsite):
  it must now match the token. `GET /api/rooms/{id}` ignored the establishment (IDOR). The JWT signing key was in the
  base config (anyone could sign a MANAGER token): no default anymore, 256 bits minimum. The logging PMS client was
  loaded in production (every notification dropped): it is now opt-in, and the app refuses to start without a client.
- **The database enforces the rules**: a partial unique index for one open task per room (no check-then-insert that two
  managers could race; its violation is a 409), composite foreign keys so a task, its room and its assignee share the
  same establishment, a check tying the times to the status.
- **Rich entity**: the status only changes through `start()` / `complete()`; completing also marks the room cleaned.
- **PMS: transactional outbox.** The task, the room and the notification are written in one transaction; a scheduled
  relay sends the notification after the commit, outside any transaction, and keeps it until the PMS has received it.
  The PMS is never told too early, and is told in the end (at least once).
- **Roles checked in the security chain**, before the body is read: a forbidden action is always a 403.
- **One clock**, injected, ticking by the microsecond like PostgreSQL. Paged list in two SQL statements (tested).

## 4. AI usage

Claude Code (Claude Opus): audit of the provided code, a first complete implementation, then this smaller rebuild
from a written specification (`CLAUDE.md`), tests first, with automated code reviews. I reviewed and committed every
change. Corrected or rejected: several review findings were rejected with a reason (entropy check of the key, a
per-day open-task rule, mapping every database error to a 409); an over-engineered first fix of the JWT key was
replaced by a minimal one; a claim that tests checked constraint names was found false and fixed; the rebuild found
an instant returned by the API that differed from the stored one (clock finer than PostgreSQL) and fixed it.

## 5. To do

- **Next**: reschedule a task to do (a forgotten task of a past day blocks its room); plan only for today or later
  and start a task only on its day, in the campsite's time zone; return a 409 when two people complete a task at the
  same time (today the second one gets a 500, and no duplicate notification thanks to the unique `task_id`); name the
  invalid fields in a 400; validate the token claims (`role: "STAFF MANAGER"` gives both roles); Swagger status codes.
- **PMS outbox**: backoff between retries, order of the notifications of a room when one fails, a lease with
  `SKIP LOCKED` for several instances, the RabbitMQ publisher, purging sent notifications.
- **Security**: RS256/JWKS with `iss`/`aud` checks; the establishment enforced in every query (Hibernate filter); the
  role checked against the database; never ship the dev key and `/dev/tokens` in the production jar.
