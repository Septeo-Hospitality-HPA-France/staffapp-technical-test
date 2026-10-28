# Technical test — StaffApp back-end developer

## Context and format

You will build a small API to manage housekeeping tasks for campsites, in 2 hours maximum. This test is a miniature
version of the StaffApp team's daily work: a need expressed by the Product Owner, an existing code base to take over,
business rules, security and tests.

- **Duration**: 2 hours of actual work. Stop after 2 hours, even if everything is not finished. Incomplete but clearly
  documented work is better than going over time.
- **AI**: you are free to use AI (assistant, agent, autocompletion) or not. Both are accepted. One condition only: you
  must be able to explain and defend every line you deliver.
- **Ambiguities**: the need is written the way a Product Owner would write it, not as a technical specification. Some
  points are deliberately left open. Make a reasonable decision and write it down in `NOTES.md`; we will not answer
  questions during the test.
- **Debrief**: a 45-minute interview follows the test. You will present your code, your choices and the way you work,
  review a colleague's Pull Request, then we will make a small change together, live, with screen sharing.

## Provided project

A ready-to-use Spring Boot project is provided: it compiles, starts, and its tests pass. You do not need to install
Java: Docker is enough to build, run and test it. Setup and commands are in the `README.md`.

The project already contains:

- **The database**: PostgreSQL via Docker Compose, schema managed by Flyway, and Testcontainers for tests.
- **Existing entities**: `Establishment` (a campsite), `Room` (an accommodation: mobile home, chalet, tent) and `Staff`
  (an employee). Each accommodation and each employee belongs to a single establishment.
- **Security**: Spring Security as a JWT resource server, already configured. The token carries the user's identifier,
  their establishment and their role (`STAFF` or `MANAGER`).
- **Establishment context**: the front end sends the current establishment in the `X-ESTABLISHMENT-ID` header, read by
  `TenantFilter`.
- **The PMS client**: `PmsClient` notifies the property management system (bookings, check-ins). It publishes to
  RabbitMQ in production; locally, it only logs.
- **Swagger UI**: once the API is started with `docker compose up --build`, open
  http://localhost:8080/swagger.html and pick a demo user: their token is filled in automatically.
- **A test helper**: `TestJwtFactory.token(user)` generates a valid token for one of the users below.

Users in the dataset:

| User   | Role    | Establishment          |
|--------|---------|------------------------|
| alice  | STAFF   | Camping Les Pins (A)   |
| bob    | STAFF   | Camping Les Pins (A)   |
| marc   | MANAGER | Camping Les Pins (A)   |
| claire | STAFF   | Camping La Plage (B)   |
| nina   | MANAGER | Camping La Plage (B)   |

Each establishment has about ten accommodations.

> [!IMPORTANT]
> **You are taking over existing code.** It was written quickly by a former team member. It works, but it is not
> perfect. Fix what matters for your feature or for the product, and list the rest in `NOTES.md`.

## The need

The Product Owner wrote the following user stories.

> **Plan the cleaning** — As a manager, I plan the cleaning of an accommodation of my campsite for a given day. I can
> assign it to one of my employees right away or later, and leave a comment ("check the terrace", "linen for 6").

> **See the day's work** — As an employee or a manager, I see the cleaning tasks of my campsite, for a given day or by
> status. In high season, a campsite has several hundred tasks a day.

> **Do the cleaning** — As an employee or a manager, I mark a task as started, then as done.

> **Hand over the accommodation** — When a task is done, the accommodation is marked as cleaned (`last_cleaned_at`)
> and the PMS is notified, so that the reception can hand the keys to arriving guests.

The Product Owner also insists on the following:

- "Never two cleaning tasks open on the same accommodation: the teams would get in each other's way."
- "A task is to do, then in progress, then done. No shortcut."
- "Only managers plan the cleaning."
- "Everyone works in their own campsite only. A campsite must never see the tasks, accommodations or employees of
  another one."
- "The PMS is what the reception trusts. If it is told an accommodation is clean when it is not, a guest walks into a
  dirty mobile home. If it is never told, the guest waits at the desk."

The data model, the API (URLs, formats, HTTP status codes) and the Flyway migrations are yours to design. Describe
your endpoints briefly in `NOTES.md`.

Security is assessed just as much as the business rules: review the provided code with that in mind too.

## Tests

The delivered code must be tested with JUnit. Focus first on the business rules and security rules: a test checking
that a forbidden action is rejected counts as much as a test of the happy path. The choice between unit tests and
integration tests is yours. A test-driven (TDD) approach is encouraged.

## Using Git

The provided project is a Git repository with a `main` branch. Follow a git flow approach. How you use Git is an
important part of the assessment.

## Deliverables

1. A **GitHub Pull Request** to `main`, with the commit history: it helps us follow your approach.
2. A `NOTES.md` file at the root, one page maximum, with five sections:
    1. **Assumptions**: the open points and what you decided.
    2. **API**: your endpoints, in a few lines.
    3. **Technical choices**: the important decisions and why, including what you changed in the existing code.
    4. **AI usage**: the tools used, what you delegated to them, what you corrected or rejected. Write "none" if you
       did not use any.
    5. **To do**: what you did not finish, what you saw in the existing code but left aside, and what you would do
       with more time.

## What we assess

- How you turn a functional need into a data model and an API.
- Compliance with the business and security rules, including in the existing code.
- Consistency of the data, including with the PMS.
- API clarity: URLs, HTTP status codes, input validation, error messages.
- Relevance of the tests.
- Code readability and the quality of the Git history.
- Your ability to prioritize and explain your choices, in writing and then at the debrief.

We are not looking for exhaustive work. We prefer a few things done well, with identified gaps, over many fragile
things.

Good luck, and see you soon for the debrief.
