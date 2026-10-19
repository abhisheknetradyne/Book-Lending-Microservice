# Library Service

A small Java service for a library desk. It keeps a catalog of books, registers members, and lets those members borrow and return books. Borrowing follows a short set of rules that a librarian can change without rebuilding the application.

The service is one Spring Boot process. Data is stored in PostgreSQL. The tables are created from `src/main/resources/schema.sql`.

## What it does

- Librarians add, edit, and remove books, and register members.
- Members and librarians borrow and return books.
- A member can borrow and return only their own loans. A librarian can do that for any member.
- Three borrowing rules are configurable:
  - a member cannot hold more than a set number of active loans
  - a member with an overdue loan cannot borrow again, while that check is turned on
  - the due date is today plus a configured number of days
- Borrowing rules live in `application.properties`, not in the database. Restart the service after changing them.
- Existing loans keep the due date they were given.
- `GET /actuator/health` reports whether the process, the database, and the disk are up.
- Metrics record loans borrowed, returned, and refused, and the latency of each API path.

## Quick start

Java 17 or newer is required. Check with `java -version`. PostgreSQL must already be running.

```sql
CREATE USER library WITH PASSWORD 'library';
CREATE DATABASE library OWNER library;
```

```bash
./mvnw spring-boot:run
```

The API listens on port **8098**. On startup the service runs `schema.sql`, then writes logs to `logs/`.

| | |
|---|---|
| Swagger UI | http://localhost:8098/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8098/v3/api-docs |
| Health | http://localhost:8098/actuator/health |

Stop the process with Ctrl+C. Start it again and the books, members, and loans are still in PostgreSQL. Borrowing rules are read from `application.properties` on each start.

To wipe the data and go back to the sample rows, stop the service and drop the tables in the `library` database. The next start runs `schema.sql` again and seeds an empty database.

## Sign in

Authentication is HTTP Basic. The accounts are in `application-security.properties`. There is no separate identity provider.

| User | Password | Role |
|---|---|---|
| `librarian` | `password` | Manages the catalog and members, views all borrow history, and can borrow or return for any member. |
| `member` | `password` | Ada Lovelace (`member@library.local`). Can read the catalog and borrow or return only her own books. |

In Swagger UI, choose **Authorize** and enter one of these pairs.

Two books are created on a fresh database: *Clean Code* and *Effective Java*.

## Try the main path

List the sample member and books (on a fresh database these ids are `1`):

```bash
curl -s -u librarian:password http://localhost:8098/api/v1/members
curl -s -u librarian:password http://localhost:8098/api/v1/books
```

Borrow as the member:

```bash
curl -s -u member:password \
  -H 'Content-Type: application/json' \
  -d '{"memberId":1,"bookId":1}' \
  http://localhost:8098/api/v1/loans
```

Return it:

```bash
curl -s -u member:password -X POST http://localhost:8098/api/v1/loans/1/return
```

Read the limits the service is enforcing. They come from `application.properties`, not from the database:

```bash
curl -s -u librarian:password \
  http://localhost:8098/api/v1/borrowing-rules
```

## REST API reference

The interactive contract is available at `/swagger-ui.html`; the OpenAPI JSON is at `/v3/api-docs`. All application endpoints use JSON and start with `/api/v1`.

### Common rules

- Authentication is HTTP Basic. Except for health, every endpoint requires credentials.
- Send `Content-Type: application/json` for request bodies.
- IDs are JSON numbers / Java `Long` values and must be positive.
- `page` is a zero-based integer, default `0`, minimum `0`.
- `size` is an integer, default `20`, minimum `1`, maximum `100`.
- Times are ISO-8601 UTC strings, for example `"2026-10-19T01:30:00Z"`.
- Paginated responses have `content` (array), `page` (integer), `size` (integer), `totalElements` (long), and `totalPages` (integer).
- Every response includes an `X-Request-Id` header.

### Request and response schemas

#### Book request

| Field | JSON type | Required | Validation |
|---|---|---|---|
| `title` | string | yes | Not blank; maximum 200 characters |
| `author` | string | yes | Not blank; maximum 200 characters |
| `isbn` | string | yes | Not blank; maximum 20 characters; unique ignoring case |
| `totalCopies` | integer | yes | From 1 through 10,000 |

#### Book response

| Field | JSON type | Nullable | Meaning |
|---|---|---|---|
| `id` | integer (64-bit) | no | Book identifier |
| `title` | string | no | Book title |
| `author` | string | no | Author name |
| `isbn` | string | no | Unique ISBN |
| `totalCopies` | integer | no | Copies owned |
| `availableCopies` | integer | no | Copies currently available |

#### Member request

| Field | JSON type | Required | Validation |
|---|---|---|---|
| `name` | string | yes | Not blank; maximum 200 characters |
| `email` | string | yes | Valid email; maximum 200 characters; unique ignoring case |

`PATCH /members/{id}` accepts only `name` with the same validation.

#### Member response

| Field | JSON type | Nullable |
|---|---|---|
| `id` | integer (64-bit) | no |
| `name` | string | no |
| `email` | string | no |

#### Loan request

| Field | JSON type | Required | Validation |
|---|---|---|---|
| `memberId` | integer (64-bit) | yes | Positive; member must exist |
| `bookId` | integer (64-bit) | yes | Positive; book must exist |

#### Loan response

| Field | JSON type | Nullable | Meaning |
|---|---|---|---|
| `id` | integer (64-bit) | no | Loan identifier |
| `bookId` | integer (64-bit) | no | Borrowed book |
| `memberId` | integer (64-bit) | no | Borrowing member |
| `borrowedAt` | ISO-8601 string | no | Borrow time |
| `dueDate` | ISO-8601 string | no | Configured due time |
| `returnedAt` | ISO-8601 string | yes | `null` while the loan is open |

### Books

#### `GET /api/v1/books`

Lists books. Any authenticated user may call it.

Parameters: query `page` (integer, optional, default `0`) and `size` (integer, optional, default `20`).

- Success: **200 OK**, `PageResponse<BookResponse>`.
- Failure: **400 Bad Request** for invalid paging; **401 Unauthorized** for missing or invalid credentials.

#### `GET /api/v1/books/{id}`

Returns one book. Any authenticated user may call it.

Parameters: path `id` (positive 64-bit integer, required).

- Success: **200 OK**, `BookResponse`.
- Failure: **400 Bad Request** for an invalid ID; **401 Unauthorized**; **404 Not Found** when the book does not exist.

#### `POST /api/v1/books`

Adds a book. Librarian only. Body: `BookRequest`.

- Success: **201 Created**, `BookResponse`.
- Failure: **400 Bad Request** for validation or malformed JSON; **401 Unauthorized**; **403 Forbidden** for a member; **409 Conflict** with `ISBN_ALREADY_EXISTS` or `DATA_CONFLICT`.

#### `PUT /api/v1/books/{id}`

Replaces the editable book values. Librarian only.

Parameters: path `id` (positive 64-bit integer, required). Body: `BookRequest`.

- Success: **200 OK**, `BookResponse`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**; **404 Not Found**; **409 Conflict** with `ISBN_ALREADY_EXISTS`, `COPIES_BELOW_LOANS`, `CONCURRENT_UPDATE`, or `DATA_CONFLICT`.

#### `DELETE /api/v1/books/{id}`

Deletes a book. Librarian only. Deletion is refused while any copy has an open loan.

Parameters: path `id` (positive 64-bit integer, required).

- Success: **204 No Content**, empty body.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**; **404 Not Found**; **409 Conflict** with `BOOK_HAS_OPEN_LOANS`.

### Members

#### `GET /api/v1/members`

Lists members. Librarian only.

Parameters: query `page` (integer, optional, default `0`) and `size` (integer, optional, default `20`).

- Success: **200 OK**, `PageResponse<MemberResponse>`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**.

#### `GET /api/v1/members/{id}`

A librarian may read any member. A member login may read only its linked member.

Parameters: path `id` (positive 64-bit integer, required).

- Success: **200 OK**, `MemberResponse`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden** when a member requests another member; **404 Not Found**.

#### `POST /api/v1/members`

Registers a member. Librarian only. Body: `MemberRequest`.

- Success: **201 Created**, `MemberResponse`.
- Failure: **400 Bad Request** for validation or malformed JSON; **401 Unauthorized**; **403 Forbidden**; **409 Conflict** with `EMAIL_ALREADY_EXISTS` or `DATA_CONFLICT`.

#### `PATCH /api/v1/members/{id}`

Changes a member name. Librarian only.

Parameters: path `id` (positive 64-bit integer, required). Body: `{ "name": "New name" }`.

- Success: **200 OK**, `MemberResponse`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**; **404 Not Found**.

### Loans

#### `GET /api/v1/loans`

A librarian can list all loans or filter by member. A member always receives only its own loans.

Parameters: query `memberId` (positive 64-bit integer, optional), `page` (integer, optional, default `0`), and `size` (integer, optional, default `20`).

- Success: **200 OK**, `PageResponse<LoanResponse>`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden** if a member requests another member.

#### `GET /api/v1/loans/{id}`

Returns one loan. A librarian can read any loan; a member can read only its own.

Parameters: path `id` (positive 64-bit integer, required).

- Success: **200 OK**, `LoanResponse`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**; **404 Not Found**.

#### `POST /api/v1/loans`

Borrows one copy. A member may borrow only for itself; a librarian may borrow for any member. Body: `LoanRequest`.

- Success: **201 Created**, `LoanResponse`.
- Failure: **400 Bad Request** for validation or malformed JSON; **401 Unauthorized**; **403 Forbidden**; **404 Not Found** for an unknown member or book; **409 Conflict** with:
  - `NO_COPIES_AVAILABLE` — no copy is available.
  - `ALREADY_BORROWED` — this member already has this book.
  - `LOAN_LIMIT_REACHED` — the member has reached the configured active-loan limit.
  - `MEMBER_HAS_OVERDUE` — an open loan is overdue.

The book row is locked during this operation. If two requests compete for the last copy, one succeeds and the other receives `NO_COPIES_AVAILABLE`.

#### `POST /api/v1/loans/{id}/return`

Returns a book. A member may return only its own loan; a librarian may return any loan. Repeating a completed return is idempotent.

Parameters: path `id` (positive 64-bit integer, required).

- Success: **200 OK**, `LoanResponse`. `returnedAt` is populated.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**; **404 Not Found**.

### Borrow history and rules

#### `GET /api/v1/borrow-history`

Returns every borrow and return record. Librarian only.

Parameters: query `page` (integer, optional, default `0`) and `size` (integer, optional, default `20`).

- Success: **200 OK**, `PageResponse<LoanResponse>`.
- Failure: **400 Bad Request**; **401 Unauthorized**; **403 Forbidden**.

#### `GET /api/v1/borrowing-rules`

Returns the rules loaded from `application.properties`. Any authenticated user may call it. It accepts no parameters.

- Success: **200 OK**:

```json
{
  "maxActiveLoans": 5,
  "blockWhenOverdue": true,
  "loanDurationDays": 14
}
```

- Failure: **401 Unauthorized**.

### Observability endpoints

#### `GET /actuator/health`

Public; no parameters.

- Success: **200 OK** when process, database, and disk checks are up.
- Failure: **503 Service Unavailable** when a health component is down.

#### `GET /actuator/metrics` and `GET /actuator/metrics/{metricName}`

Any authenticated user. `metricName` is a string. The detail endpoint also accepts repeatable query parameter `tag` as a string in `key:value` format.

- Success: **200 OK**, Spring Boot Actuator metric JSON.
- Failure: **401 Unauthorized**; **404 Not Found** for an unknown metric.

### Error responses

Application errors use this schema:

```json
{
  "timestamp": "2026-10-19T01:30:00Z",
  "path": "/api/v1/loans",
  "code": "LOAN_LIMIT_REACHED",
  "message": "Member already has 5 active loans",
  "fieldErrors": []
}
```

`timestamp`, `path`, `code`, and `message` are strings. `fieldErrors` is an array of `{ "field": string, "message": string }`.

Common status codes:

| HTTP status | Error code | Meaning |
|---|---|---|
| **400 Bad Request** | `VALIDATION_FAILED` | A body, path, or query value violates validation |
| **400 Bad Request** | `MALFORMED_REQUEST` | The JSON body is missing or malformed |
| **400 Bad Request** | `MALFORMED_PARAMETER` | A path or query value has the wrong type |
| **401 Unauthorized** | `UNAUTHORIZED` | Basic credentials are missing or invalid |
| **403 Forbidden** | `FORBIDDEN` | The authenticated user cannot perform the operation |
| **404 Not Found** | `NOT_FOUND` | The requested entity does not exist |
| **409 Conflict** | endpoint-specific code | A business rule or database constraint rejected the operation |

## Borrowing rules

Each borrow loads the member, the book, and that member's open loans, then runs the rules in order. The first refusal stops the borrow.

1. A copy is on the shelf.
2. The member does not already have this book out.
3. Open loans are below `maxActiveLoans`. An open loan is one whose `returnedAt` is empty.
4. If `blockWhenOverdue` is true, none of those loans have a `dueDate` in the past.

If they all pass, the due date is the borrow time plus `loanDurationDays`.

`src/main/resources/application.properties` holds the starting values:

```properties
library.borrowing.max-active-loans=5
library.borrowing.block-when-overdue=true
library.borrowing.loan-duration-days=14
```

Those values are the rules. They are not stored in the database. Change them in `application.properties` and restart the service. Every instance that shares that configuration enforces the same limit.

A new kind of restriction is a new `BorrowingRule` class. The borrow path does not need a new branch.

## Security

`SecurityConfig` loads the users from `library.security.users` in `application-security.properties` and stores bcrypt hashes in memory. Passwords in that file are the ones clients send. They are hashed when the process starts. Log settings are in `application-logging.properties`.

A member login has `member-email`. That email must match a member row. The sample login matches Ada Lovelace.

## Observability

`GET /actuator/health` is the only health API. It is up only when all three checks are up:

- `ping` — the process answered
- `db` — the database accepts a connection
- `diskSpace` — free space is above 10MB

Useful metrics:

| Metric | Meaning |
|---|---|
| `library.loans.borrowed` | Loans created |
| `library.loans.returned` | Books returned |
| `library.loans.rejected` | Refusals, tagged with the rule `code` |
| `library.loans.active` | Open loans right now |
| `library.loans.borrow` | Time spent inside the borrow operation |
| `http.server.requests` | Latency of each API path, tagged with `method` and `uri` |

```bash
curl -s -u librarian:password \
  'http://localhost:8098/actuator/metrics/http.server.requests?tag=method:POST&tag=uri:/api/v1/loans'
```

Each API response includes `X-Request-Id`. The same id is on the log line for that call. Logs go to the terminal and to `logs/library-service.log`.

## How the code is organized

```text
controller     HTTP endpoints
dto            request and response records
entity         books, members, and loans
exception      error types and the API error handler
service        use-case interfaces
serviceimpl    use-case implementations
domain/rules   borrowing rules
repository     JPA SqlSession. Connections come from the Hikari pool
config         security, properties, seeding, metrics, OpenAPI
```

Controllers depend on the service interfaces. Those implementations call `sqlSession`, which uses the JPA session. Each database call checks a connection out of the Hikari pool.

## Tests

```bash
./mvnw test
```

Rule tests cover each borrowing decision without starting the server. The API test uses the local PostgreSQL database `library_test` and walks through security, borrow, return, the loan limit, overdue loans, health, and metrics.

## Configuration

| Property | Purpose |
|---|---|
| `server.port` | HTTP port. Default `8098` |
| `library.borrowing.*` | Borrowing rules. Not stored in the database. Restart after a change. |
| `library.security.users[*]` | Usernames, passwords, roles, and the member email for a member login. File: `application-security.properties` |
| `logging.file.name` | Log file path. File: `application-logging.properties` |
| `logging.level.*` | Log levels. File: `application-logging.properties` |
| `spring.datasource.url` | PostgreSQL database. Default `jdbc:postgresql://localhost:5432/library` |
| `spring.datasource.hikari.*` | Connection pool size and timeout. The JPA session borrows connections from this pool. |
