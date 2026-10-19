# Book Lending Microservice

## 1. Objective

A secured Java microservice for a library desk. It manages the book catalog, registers members, and lets a member borrow and return books. Borrowing rules are configurable and are not stored in the database.

## 2. Key features

- Catalog of books, with a unique ISBN and a count of copies still on the shelf.
- Member records, with a unique email.
- Borrow and return, including a full borrow history for the librarian.
- Configurable borrowing rules from `application.properties` (change config and redeploy to update).
- HTTP Basic authentication with two predefined roles.
- Request validation, centralized error responses, health checks, metrics, and request ids.
- PostgreSQL persistence. Tables are created from `src/main/resources/schema.sql` on startup (see **Database schema**).
- HikariCP connection pooling for PostgreSQL access.
- Persistence: `com.library.session.SqlSession` (save, find, delete, Hibernate `session()`) and `com.library.dao` classes (`BookDao`, `MemberDao`, `LoanDao`, `HealthDao`, `MetricDao`), each injecting `SqlSession` and owning its queries.
- Borrow eligibility checks live in `com.library.rules` (`BorrowingRules`, `BorrowingContext`, `RuleResult`).
- Swagger UI and automated tests.

## 3. Roles allowed

| Role | Username | Password | Allowed operations |
|---|---|---|---|
| `LIBRARIAN` | `librarian` | `DeskAdmin!26` | Manage books and members, view all loans and borrow history, borrow and return for any member |
| `MEMBER` | `member@library.local` (sample login; use the member's **email** for others) | `BorrowSelf!26` (sample account; new members use the password chosen at registration) | Read the catalog and the borrowing rules, and borrow, return, and read only their own loans |

The sample member is Ada Lovelace at `member@library.local` with password **`BorrowSelf!26`** (from `library.seed.member-password` on startup). When a librarian registers another member, that person signs in with their **email + password** from `POST /api/v1/members`.

## 4. Data model

### Book

| Field | Type | Description |
|---|---|---|
| `id` | number | Primary key |
| `title` | string | Book title, maximum 200 characters |
| `author` | string | Author, maximum 200 characters |
| `isbn` | string | Unique ISBN, maximum 20 characters |
| `totalCopies` | number | Copies owned by the library |
| `availableCopies` | number | Copies currently on the shelf |

### Member

| Field | Type | Description |
|---|---|---|
| `id` | number | Primary key |
| `name` | string | Member name, maximum 200 characters |
| `email` | string | Unique email, maximum 200 characters |

### Loan

| Field | Type | Description |
|---|---|---|
| `id` | number | Primary key |
| `bookId` | number | Foreign key to Book |
| `memberId` | number | Foreign key to Member |
| `borrowedAt` | string | Borrow time, UTC, `yyyy-MM-dd HH:mm:ss` |
| `dueDate` | string | Due time, UTC, same format |
| `returnedAt` | string or null | Return time, same format. `null` while the loan is open |

## 5. Technical stack

- Java 17
- Spring Boot 4
- Spring Web, Spring Security, Spring Data JPA, Hibernate
- Bean Validation
- PostgreSQL
- HikariCP connection pool
- BCrypt password hashing
- springdoc OpenAPI / Swagger UI
- Spring Boot Actuator (health only); loan metrics in PostgreSQL

## 6. Prerequisites

| Dependency | Version | Purpose |
|---|---|---|
| Java JDK | 17 or newer | Build and run the service |
| PostgreSQL | 14+ (16 recommended) | Books, members, and loans |
| Maven | Optional | The project includes `./mvnw` |

Connection pooling uses **HikariCP** (via Spring Boot). Defaults are in `application.properties`: maximum pool size 10, minimum idle 2, connection timeout 30 seconds.

## 7. Database schema

Hibernate is set to `spring.jpa.hibernate.ddl-auto=validate`, so it does **not** create tables by itself. On every startup, Spring runs `src/main/resources/schema.sql` because `spring.sql.init.mode=always`. That script uses `CREATE TABLE IF NOT EXISTS`, so existing databases are left intact.

| Table | Purpose |
|---|---|
| `books` | Catalog; unique `isbn`; `total_copies` and `available_copies` |
| `members` | Registered members; unique `email`; `password_hash` (BCrypt) for HTTP Basic login |
| `loans` | Borrow and return history; foreign keys to `members` and `books` |
| `library_metric_counters` | Cumulative loan event counts (`borrowed`, `returned`, `rejected` with rule `code`) |
| `library_metric_timers` | Running totals for borrow duration (`library.loans.borrow`) |

Borrowing rules are **not** stored in the database. They are read from `application.properties` at startup (see section 10).

## 8. Getting started

### Option A — Docker Compose (PostgreSQL + service)

Requires Docker and Docker Compose.

```bash
git clone https://github.com/abhisheknetradyne/Book-Lending-Microservice.git
cd Book-Lending-Microservice
docker compose up --build
```

Wait until the app log shows `Started LibraryServiceApplication`. The API is at http://localhost:8098. PostgreSQL is exposed on port 5432 with user `library`, password `library`, database `library`.

### Option B — Local PostgreSQL and Maven

Create databases once:

```sql
CREATE USER library WITH PASSWORD 'library';
CREATE DATABASE library OWNER library;
CREATE DATABASE library_test OWNER library;
```

Start PostgreSQL, then from the project root (Java 17 on your `PATH`):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS example
./mvnw spring-boot:run
```

The service listens on http://localhost:8098. On startup it applies `schema.sql` and seeds Ada Lovelace plus two sample books when those rows are missing.

| | |
|---|---|
| Swagger UI | http://localhost:8098/swagger-ui.html |
| OpenAPI JSON | http://localhost:8098/v3/api-docs |
| Liveness | http://localhost:8098/actuator/health/liveness |
| Readiness | http://localhost:8098/actuator/health/readiness |

In Swagger, choose **Authorize** and enter `librarian` / `DeskAdmin!26` or a member email / that member's password (e.g. `member@library.local` / `BorrowSelf!26` for the seed account).

Tests use the `library_test` database (same user and password):

```bash
./mvnw test
```

## 9. Security

- Use HTTPS outside local development.
- Every `/api/v1` call requires HTTP Basic credentials. Health is public.
- Passwords in `application-security.properties` are hashed with BCrypt when the process starts.
- A member can act only on the member row linked to their login.

## 10. Borrowing rules

Rules are defined in `application.properties` and loaded when the process starts. To change them, edit that file (or override the same keys via environment variables / deployment config) and **restart or redeploy** the service. There is no API to write rules at runtime.

```properties
library.borrowing.max-active-loans=5
library.borrowing.block-when-overdue=true
library.borrowing.loan-duration-days=14
```

`GET /api/v1/borrowing-rules` exposes the active values for clients; it does not change configuration.

On each borrow, the service locks the book row and then checks, in order:

1. At least one copy is available.
2. This member does not already have this book out.
3. Open loans are below `max-active-loans`.
4. If `block-when-overdue` is true, no open loan has a due date in the past.

The due date is the borrow time plus `loan-duration-days`. A loan that already exists keeps its original due date.

## 11. Observability

Each API response includes `X-Request-Id`. The same id is written on the log line for that call. Logs go to the terminal and to `logs/library-service.log`. Default level for `com.library` is **INFO** (`application-logging.properties`).

### Logging (what you see per call)

| Source | Level | When |
|---|---|---|
| `RequestIdFilter` | INFO | Every `/api/*` request when it finishes: `method`, `path`, `status` |
| `ApiExceptionHandler` | WARN | Validation failures (field list), 4xx API errors (`code`, `message`, `path`) |
| `ApiExceptionHandler` | ERROR | 5xx and unexpected exceptions (stack trace) |
| Service layer (`*ServiceImpl`) | INFO | Mutations started and succeeded (book/member/loan) |
| Service layer | WARN | Business failures before throw (not found, conflict, borrow rejected, invalid member) |
| `LoanServiceImpl` | INFO | Borrow/return lifecycle (including idempotent return) |
| `HealthServiceImpl` | INFO / ERROR | Readiness checks (same as before) |
| `DataSeeder` | INFO | One-time seed messages at startup |

Every log line for an API call includes the request id from MDC when the filter has set it.

### Metrics (PostgreSQL, not logs)

Every **`/api/*`** request (when the filter finishes) records:

- **HTTP status** — counter `library.api.responses` keyed by status code (`200`, `400`, `409`, …)
- **End-to-end latency** — timer `library.api.request` (receive → response complete, milliseconds)

Loan-specific counters are updated inside loan services as before:

| API | Loan metrics written |
|---|---|
| `POST /api/v1/loans` (success) | `library.loans.borrowed` +1; `library.loans.borrow` timer (service handling ms) |
| `POST /api/v1/loans` (rule rejection) | `library.loans.rejected` +1 for that rule `code` (persists even though the borrow transaction rolls back) |
| `POST /api/v1/loans/return` (return processed) | `library.loans.returned` +1 |

`GET /actuator/health/liveness` checks that the process is running. `GET /actuator/health/readiness` runs `SELECT 1` against PostgreSQL and checks free disk space.

Loan metrics are **stored in PostgreSQL** (`library_metric_counters`, `library_metric_timers`). Open loans are counted from the `loans` table. Read them with **`GET /api/v1/metrics`** (any signed-in user).

| Response field | Description |
|---|---|
| `loansBorrowed` | Successful borrows since the database was created |
| `loansReturned` | Returns recorded in metrics |
| `loansActive` | Open loans (`returned_at` is null) |
| `loansRejected` | Borrow refusals, each with `code` and `count` |
| `borrowDuration` | `sampleCount` and `averageMs` for successful borrow handling |
| `httpResponses` | API responses by HTTP `status` and `count` |
| `apiRequestDuration` | `sampleCount` and `averageMs` for full `/api/*` request latency |

## 12. API details

All application APIs are under `/api/v1` and use JSON.

### cURL

Local base URL: `http://localhost:8098`. Use HTTP Basic auth with `-u 'username:password'`. Examples below use the seed librarian (`librarian` / `DeskAdmin!26`) unless the endpoint is member-specific; swap in `member@library.local` / `BorrowSelf!26` for member-only flows. Health endpoints need no credentials. Add `-i` to see response headers (including `X-Request-Id`).

### Common headers

| Header | Mandatory | Description |
|---|---|---|
| `Authorization` | Yes, except health | `Basic` credentials for `librarian` or `member` |
| `Content-Type` | Yes, when a body is sent | `application/json` |

### Common error response

```json
{
  "timestamp": "2026-10-19 01:30:00",
  "path": "/api/v1/loans",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "fieldErrors": [
    {
      "field": "isbn",
      "message": "must not be blank"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `timestamp` | string | UTC, `yyyy-MM-dd HH:mm:ss` |
| `path` | string | Request path |
| `code` | string | Stable error code |
| `message` | string | Human-readable message |
| `fieldErrors` | array | Empty unless a field failed validation. Each item has `field` and `message` strings |

| HTTP status | Code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | A body, path, or query value breaks a validation rule |
| 400 | `MALFORMED_REQUEST` | The JSON body is missing or cannot be read |
| 400 | `MALFORMED_PARAMETER` | A path or query value has the wrong type |
| 401 | `UNAUTHORIZED` | Credentials are missing or wrong |
| 403 | `FORBIDDEN` | The signed-in user cannot perform this operation |
| 404 | `NOT_FOUND` | The book, member, or loan does not exist |
| 409 | endpoint code | A business rule or database constraint rejected the request |

Paging parameters, used by every list endpoint:

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `page` | query | number | No | Zero-based page. Default `0`. Minimum `0` |
| `size` | query | number | No | Page size. Default `20`. Minimum `1`. Maximum `100` |

A paged response contains `content` (array), `page` (number), `size` (number), `totalElements` (number), and `totalPages` (number).

### `GET /api/v1/books`

List books. Any signed-in user.

**Query parameters:** `page`, `size`.

```bash
curl -sS -u 'librarian:DeskAdmin!26' \
  'http://localhost:8098/api/v1/books?page=0&size=20'
```

**Success — 200**

```json
{
  "content": [
    {
      "id": 1,
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "isbn": "9780132350884",
      "totalCopies": 3,
      "availableCopies": 3
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Failure:** 400 `VALIDATION_FAILED` for an invalid page or size. 401 `UNAUTHORIZED`.

### `GET /api/v1/books/{id}`

One book. Any signed-in user.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Book id. Must be a positive whole number |

```bash
curl -sS -u 'librarian:DeskAdmin!26' \
  'http://localhost:8098/api/v1/books/1'
```

**Success — 200:** one book object, same fields as an item in the list above.

**Failure:** 400 `MALFORMED_PARAMETER` or `VALIDATION_FAILED`. 401 `UNAUTHORIZED`. 404 `NOT_FOUND`.

### `POST /api/v1/books`

Add a book. Librarian only.

| Field | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `title` | body | string | Yes | Not blank, maximum 200 characters |
| `author` | body | string | Yes | Not blank, maximum 200 characters |
| `isbn` | body | string | Yes | Not blank, maximum 20 characters, unique ignoring case |
| `totalCopies` | body | number | Yes | Integer from 1 through 10000 |

```json
{
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "isbn": "9780134685991",
  "totalCopies": 2
}
```

```bash
curl -sS -u 'librarian:DeskAdmin!26' -X POST 'http://localhost:8098/api/v1/books' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Effective Java","author":"Joshua Bloch","isbn":"9780134685991","totalCopies":2}'
```

**Success — 201:** the created book, including `id` and `availableCopies`.

**Failure:** 400 `VALIDATION_FAILED` or `MALFORMED_REQUEST`. 401 `UNAUTHORIZED`. 403 `FORBIDDEN`. 409 `ISBN_ALREADY_EXISTS` or `DATA_CONFLICT`.

### `PUT /api/v1/books/{id}`

Replace the title, author, ISBN, and total copies. Librarian only. Total copies cannot fall below the number currently on loan.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive book id |
| body | body | object | Yes | Same fields and validation as `POST /api/v1/books` |

```bash
curl -sS -u 'librarian:DeskAdmin!26' -X PUT 'http://localhost:8098/api/v1/books/1' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Effective Java","author":"Joshua Bloch","isbn":"9780134685991","totalCopies":2}'
```

**Success — 200:** the updated book.

**Failure:** 400, 401, 403, 404. 409 `ISBN_ALREADY_EXISTS`, `COPIES_BELOW_LOANS`, `CONCURRENT_UPDATE`, or `DATA_CONFLICT`.

### `DELETE /api/v1/books/{id}`

Delete a book. Librarian only. Refused while any copy is on loan.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive book id |

```bash
curl -sS -u 'librarian:DeskAdmin!26' -X DELETE 'http://localhost:8098/api/v1/books/1'
```

**Success — 204:** empty body.

**Failure:** 400, 401, 403, 404. 409 `BOOK_HAS_OPEN_LOANS`.

### `GET /api/v1/members`

List members. Librarian only.

**Query parameters:** `page`, `size`.

```bash
curl -sS -u 'librarian:DeskAdmin!26' \
  'http://localhost:8098/api/v1/members?page=0&size=20'
```

**Success — 200**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Ada Lovelace",
      "email": "member@library.local"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Failure:** 400, 401, 403.

### `POST /api/v1/members`

Register a member. Librarian only. Creates the member record and a login (HTTP Basic: **email** + **password**).

| Field | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `name` | body | string | Yes | Not blank, maximum 200 characters |
| `email` | body | string | Yes | Valid email, maximum 200 characters, unique ignoring case |
| `password` | body | string | Yes | 8–72 characters (stored as BCrypt hash) |

```json
{
  "name": "Grace Hopper",
  "email": "grace@library.local",
  "password": "GraceReads!26"
}
```

```bash
curl -sS -u 'librarian:DeskAdmin!26' -X POST 'http://localhost:8098/api/v1/members' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grace Hopper","email":"grace@library.local","password":"GraceReads!26"}'
```

**Success — 201:** the created member.

**Failure:** 400, 401, 403. 409 `EMAIL_ALREADY_EXISTS` or `DATA_CONFLICT`.

### `GET /api/v1/members/{id}`

A librarian can read any member. A member can read only their own record.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive member id |

```bash
# Librarian — any member id
curl -sS -u 'librarian:DeskAdmin!26' 'http://localhost:8098/api/v1/members/1'

# Member — own id only
curl -sS -u 'member@library.local:BorrowSelf!26' 'http://localhost:8098/api/v1/members/1'
```

**Success — 200:** one member object.

**Failure:** 400, 401, 404. 403 when a member asks for someone else.

### `PATCH /api/v1/members/{id}`

Change a member name. Librarian only. Email cannot be changed here.

| Parameter | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive member id |
| `name` | body | string | Yes | Not blank, maximum 200 characters |

```json
{
  "name": "Ada Lovelace"
}
```

```bash
curl -sS -u 'librarian:DeskAdmin!26' -X PATCH 'http://localhost:8098/api/v1/members/1' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace"}'
```

**Success — 200:** the updated member.

**Failure:** 400, 401, 403, 404.

### `POST /api/v1/loans`

Borrow one copy. A member may borrow only for their own member id. A librarian may borrow for any member.

| Field | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `memberId` | body | number | Yes | Positive, and the member must exist |
| `bookId` | body | number | Yes | Positive, and the book must exist |

```json
{
  "memberId": 1,
  "bookId": 1
}
```

```bash
# Member borrowing for themselves
curl -sS -u 'member@library.local:BorrowSelf!26' -X POST 'http://localhost:8098/api/v1/loans' \
  -H 'Content-Type: application/json' \
  -d '{"memberId":1,"bookId":1}'

# Librarian borrowing on behalf of a member
curl -sS -u 'librarian:DeskAdmin!26' -X POST 'http://localhost:8098/api/v1/loans' \
  -H 'Content-Type: application/json' \
  -d '{"memberId":1,"bookId":1}'
```

**Success — 201**

```json
{
  "id": 10,
  "bookId": 1,
  "memberId": 1,
  "borrowedAt": "2026-10-19 01:30:00",
  "dueDate": "2026-11-02 01:30:00",
  "returnedAt": null
}
```

**Failure:** 400, 401, 403, 404.

| 409 code | Message |
|---|---|
| `NO_COPIES_AVAILABLE` | No copies of this book are available |
| `ALREADY_BORROWED` | This member already has an open loan for the book |
| `LOAN_LIMIT_REACHED` | Member already has 5 active loans |
| `MEMBER_HAS_OVERDUE` | Member has an overdue loan |

The 5 and the 14-day due date come from configuration. If two requests compete for the last copy, the book row is locked: one request returns 201 and the other returns 409 `NO_COPIES_AVAILABLE`.

### `POST /api/v1/loans/return`

Return a book. Send the same `memberId` and `bookId` used to borrow. A member may return only their own loan. A librarian may return any loan. Returning a loan that is already returned does not change it.

| Field | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `memberId` | body | number | Yes | Positive, and the member must exist |
| `bookId` | body | number | Yes | Positive, and the book must exist |

```json
{
  "memberId": 1,
  "bookId": 1
}
```

```bash
curl -sS -u 'member@library.local:BorrowSelf!26' -X POST 'http://localhost:8098/api/v1/loans/return' \
  -H 'Content-Type: application/json' \
  -d '{"memberId":1,"bookId":1}'
```

**Success — 200:** the loan, with `returnedAt` set. A second return returns the same loan and still 200.

**Failure:** 400, 401, 403, 404.

### `GET /api/v1/borrow-history`

Borrowing history for one member, including returned loans. `memberId` is required. A librarian can read any member. A member can read only their own.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `memberId` | query | number | Yes | Member id. Missing or unknown ids return 400 `INVALID_MEMBER` |
| `page` | query | number | No | Page, default 0 |
| `size` | query | number | No | Page size, default 20 |

```bash
curl -sS -u 'member@library.local:BorrowSelf!26' \
  'http://localhost:8098/api/v1/borrow-history?memberId=1&page=0&size=20'
```

**Success — 200:** a page of loan objects, including returned loans.

**Failure:** 400 `INVALID_MEMBER` with message `invalid member id`. 401. 403 when a member requests another member's loans.

### `GET /api/v1/borrowing-rules`

The rules loaded from configuration. Any signed-in user.

```bash
curl -sS -u 'member@library.local:BorrowSelf!26' \
  'http://localhost:8098/api/v1/borrowing-rules'
```

**Success — 200**

```json
{
  "maxActiveLoans": 5,
  "blockWhenOverdue": true,
  "loanDurationDays": 14
}
```

**Failure:** 401 `UNAUTHORIZED`.

### `GET /actuator/health/liveness`

Public. No parameters and no body. This only checks that the process answered. It does not call the database.

```bash
curl -sS 'http://localhost:8098/actuator/health/liveness'
```

**Success — 200**

```json
{
  "healthy": true
}
```

**Failure:** no JSON if the process is down. The connection is refused.

### `GET /actuator/health/readiness`

Public. No parameters and no body. Ready means PostgreSQL answered `SELECT 1` and free disk space is at least 10 MB. `healthy` is true only when both checks passed.

```bash
curl -sS 'http://localhost:8098/actuator/health/readiness'
```

**Success — 200**

```json
{
  "healthy": true,
  "databaseCheck": true,
  "diskCheck": true
}
```

**Failure — 503:** the same shape, with `healthy` false and the failed check false.

| Field | Type | Description |
|---|---|---|
| `healthy` | boolean | True when every check in that response passed |
| `databaseCheck` | boolean | True when `SELECT 1` succeeded. Readiness only |
| `diskCheck` | boolean | True when free disk space is at least 10 MB. Readiness only |

### `GET /api/v1/metrics`

Loan counters and borrow timings from PostgreSQL. Any signed-in user.

```bash
curl -sS -u 'librarian:DeskAdmin!26' 'http://localhost:8098/api/v1/metrics'
```

**Success — 200**

```json
{
  "loansBorrowed": 12,
  "loansReturned": 8,
  "loansActive": 4,
  "loansRejected": [
    { "code": "LOAN_LIMIT_REACHED", "count": 2 }
  ],
  "borrowDuration": {
    "sampleCount": 12,
    "averageMs": 45.5
  },
  "httpResponses": [
    { "status": 200, "count": 840 },
    { "status": 400, "count": 12 },
    { "status": 409, "count": 5 }
  ],
  "apiRequestDuration": {
    "sampleCount": 857,
    "averageMs": 18.2
  }
}
```

**Failure:** 401.

## 13. Edge cases

| Case | Result |
|---|---|
| Two members request the last copy at the same time | One receives 201. The other receives 409 `NO_COPIES_AVAILABLE` |
| The same member borrows the same book twice while the first loan is open | 409 `ALREADY_BORROWED` |
| The member already has the configured number of open loans | 409 `LOAN_LIMIT_REACHED` |
| An open loan is past its due date and blocking is enabled | 409 `MEMBER_HAS_OVERDUE` |
| A book is deleted while a copy is still out | 409 `BOOK_HAS_OPEN_LOANS` |
| Total copies would drop below copies on loan | 409 `COPIES_BELOW_LOANS` |
| Return is called again after the book is back | 200, and the loan is unchanged |
| A member calls a librarian operation | 403 `FORBIDDEN` |
