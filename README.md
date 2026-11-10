# Housekeeping API — StaffApp technical test

API to manage the housekeeping tasks of a campsite. This project is the starting point of the test: read the brief in
[`candidate-brief.md`](candidate-brief.md) first.

## Prerequisites

- Docker, nothing else: the app is built and tested in containers (see the commands below).

If you have Java 21 or later, you can also build and test directly on your machine. Maven is not required: the
`./mvnw` wrapper is provided (`mvnw.cmd` on Windows).

## Commands

```bash
./mvnw clean test                 # runs the tests (Testcontainers starts PostgreSQL on its own)
docker compose up -d postgres     # starts PostgreSQL only, port 5433
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"   # starts the API on http://localhost:8080
```

The JWT signing key has no default value: the application refuses to start without it. The `dev` profile and the
tests provide a local key. Anywhere else, set `APP_SECURITY_JWT_SECRET` to a random value of at least 32 bytes, for
example `openssl rand -base64 48`.

With Docker only, without Java on your machine:

```bash
docker compose up --build         # builds the app image and starts PostgreSQL + the API on http://localhost:8080
docker compose run --rm test      # runs the tests in a container
```

The schema is created and the demo data loaded by Flyway at startup (`src/main/resources/db/migration`).

## Structure

| Package         | Content                                                                                                |
|-----------------|--------------------------------------------------------------------------------------------------------|
| `establishment` | `Establishment`: a campsite                                                                            |
| `room`          | `Room` (an accommodation) and the `GET /api/rooms` and `GET /api/rooms/{id}` endpoints                 |
| `pms`           | `PmsClient`: notifies the PMS (bookings, check-ins). Logs locally (`app.pms.client=logging`, never loaded by default). The RabbitMQ publisher is not written yet |
| `staff`         | `Staff`: an employee, identified by their `username`                                                   |
| `security`      | Spring Security configuration (JWT)                                                                    |
| `tenant`        | `TenantFilter` and `CurrentEstablishment`: the establishment of the current request                    |
| `dev`           | `dev` profile only: Swagger UI configuration and a token per demo user (`GET /dev/tokens`)             |

## Authentication

Every call to `/api/**` expects two headers:

```
Authorization: Bearer <token>
X-ESTABLISHMENT-ID: <establishment id>
```

The token is a signed JWT (HS256) carrying:

| Claim              | Content                                                                      | Example |
|--------------------|------------------------------------------------------------------------------|---------|
| `sub`              | the employee's username                                                      | `alice` |
| `establishment_id` | the id of their establishment                                                | `1`     |
| `role`             | `STAFF` or `MANAGER`, exposed as the `ROLE_STAFF` / `ROLE_MANAGER` authority | `STAFF` |

Alice's token, decoded:

```json
// Header
{
  "alg": "HS256"
}
// Payload
{
  "sub": "alice",
  "establishment_id": 1,
  "role": "STAFF",
  "iat": 1793185001,
  "exp": 1793188601
}
```

`iat` and `exp` are Unix timestamps: the token is valid for one hour.

In tests, `TestJwtFactory.token("alice")` generates a valid token. To call the API by hand, use Swagger UI (see
below): it fills in the token of the selected user.

## Demo data

| User   | Role    | Establishment           |
|--------|---------|-------------------------|
| alice  | STAFF   | Camping Les Pins (id 1) |
| bob    | STAFF   | Camping Les Pins (id 1) |
| marc   | MANAGER | Camping Les Pins (id 1) |
| claire | STAFF   | Camping La Plage (id 2) |
| nina   | MANAGER | Camping La Plage (id 2) |

Each establishment has 10 accommodations: `A01` to `A10` (ids 1 to 10) for Les Pins, `B01` to `B10` (ids 11 to 20)
for La Plage. The ids are also in `DemoData`, on the test side.

## Calling the API

### Swagger UI

Start the API (`docker compose up --build`) and open http://localhost:8080/swagger.html. Pick a demo user at the top
of the page: their token and establishment are added to every call. A value entered through *Authorize* takes
precedence, to try other combinations.

Swagger UI is only served with the `dev` profile, enabled by `docker compose`. With Java on your machine:
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.