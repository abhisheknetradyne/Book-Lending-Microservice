# Book Lending Microservice

## 1. Objective

A secured Java microservice for a library desk. It manages the book catalog, registers members, and lets a member borrow and return books. Borrowing rules are configurable and are not stored in the database.

## 2. Key features

- Catalog of books, with a unique ISBN and a count of copies still on the shelf.
- Member records, with a unique email.
- Borrow and return, including a full borrow history for the librarian.
- Configurable rules: maximum active loans, block when overdue, and loan duration.
- HTTP Basic authentication with two predefined roles.
- Request validation, centralized error responses, health checks, metrics, and request ids.
- PostgreSQL persistence, with the schema in `src/main/resources/schema.sql`.
- Swagger UI and automated tests.

## 3. Roles allowed

| Role | Username | Password | Allowed operations |
|---|---|---|---|
| `LIBRARIAN` | `librarian` | `password` | Manage books and members, view all loans and borrow history, borrow and return for any member |
| `MEMBER` | `member` | `password` | Read the catalog and the borrowing rules, and borrow, return, and read only their own loans |

The member login is linked to `member@library.local` (Ada Lovelace). A member name is not a username.

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
| `borrowedAt` | string | Borrow time, ISO-8601 UTC |
| `dueDate` | string | Due time, calculated from the configured duration |
| `returnedAt` | string or null | Return time. `null` while the loan is open |

## 5. Technical stack

- Java 17
- Spring Boot 4
- Spring Web, Spring Security, Spring Data JPA, Hibernate
- Bean Validation
- PostgreSQL
- HikariCP connection pool
- BCrypt password hashing
- springdoc OpenAPI / Swagger UI
- Micrometer and Spring Boot Actuator

## 6. Getting started

Java 17 or newer and PostgreSQL are required.

```sql
CREATE USER library WITH PASSWORD 'library';
CREATE DATABASE library OWNER library;
CREATE DATABASE library_test OWNER library;
```

```bash
git clone https://github.com/abhisheknetradyne/Book-Lending-Microservice.git
cd Book-Lending-Microservice
./mvnw spring-boot:run
```

The service listens on http://localhost:8098. On startup it applies `schema.sql` and seeds Ada Lovelace plus two sample books when those rows are missing.

| | |
|---|---|
| Swagger UI | http://localhost:8098/swagger-ui.html |
| OpenAPI JSON | http://localhost:8098/v3/api-docs |
| Health | http://localhost:8098/actuator/health |

In Swagger, choose **Authorize** and enter `librarian` / `password` or `member` / `password`.

Tests use the `library_test` database:

```bash
./mvnw test
```

## 7. Security

- Use HTTPS outside local development.
- Every `/api/v1` call requires HTTP Basic credentials. Health is public.
- Passwords in `application-security.properties` are hashed with BCrypt when the process starts.
- A member can act only on the member row linked to their login.

## 8. Borrowing rules

These values are in `application.properties`. They are not stored in the database. Change them and restart the service.

```properties
library.borrowing.max-active-loans=5
library.borrowing.block-when-overdue=true
library.borrowing.loan-duration-days=14
```

On each borrow, the service locks the book row and then checks, in order:

1. At least one copy is available.
2. This member does not already have this book out.
3. Open loans are below `max-active-loans`.
4. If `block-when-overdue` is true, no open loan has a due date in the past.

The due date is the borrow time plus `loan-duration-days`. A loan that already exists keeps its original due date.

## 9. Observability

Each API response includes `X-Request-Id`. The same id is written on the log line for that call. Logs go to the terminal and to `logs/library-service.log`.

`GET /actuator/health` is up only when the process, the database, and disk space are up.

| Metric | Description |
|---|---|
| `library.loans.borrowed` | Loans created |
| `library.loans.returned` | Books returned |
| `library.loans.rejected` | Refusals, tagged with the rule code |
| `library.loans.active` | Open loans |
| `library.loans.borrow` | Time spent inside borrow |
| `http.server.requests` | Latency of each API path |

## 10. API details

All application APIs are under `/api/v1` and use JSON.

### Common headers

| Header | Mandatory | Description |
|---|---|---|
| `Authorization` | Yes, except health | `Basic` credentials for `librarian` or `member` |
| `Content-Type` | Yes, when a body is sent | `application/json` |

### Common error response

```json
{
  "timestamp": "2026-10-19T01:30:00Z",
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
| `timestamp` | string | UTC time of the error |
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

**Success — 201:** the created book, including `id` and `availableCopies`.

**Failure:** 400 `VALIDATION_FAILED` or `MALFORMED_REQUEST`. 401 `UNAUTHORIZED`. 403 `FORBIDDEN`. 409 `ISBN_ALREADY_EXISTS` or `DATA_CONFLICT`.

### `PUT /api/v1/books/{id}`

Replace the title, author, ISBN, and total copies. Librarian only. Total copies cannot fall below the number currently on loan.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive book id |
| body | body | object | Yes | Same fields and validation as `POST /api/v1/books` |

**Success — 200:** the updated book.

**Failure:** 400, 401, 403, 404. 409 `ISBN_ALREADY_EXISTS`, `COPIES_BELOW_LOANS`, `CONCURRENT_UPDATE`, or `DATA_CONFLICT`.

### `DELETE /api/v1/books/{id}`

Delete a book. Librarian only. Refused while any copy is on loan.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive book id |

**Success — 204:** empty body.

**Failure:** 400, 401, 403, 404. 409 `BOOK_HAS_OPEN_LOANS`.

### `GET /api/v1/members`

List members. Librarian only.

**Query parameters:** `page`, `size`.

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

Register a member. Librarian only. This creates a member record. It does not create a login.

| Field | In | Type | Mandatory | Validation |
|---|---|---|---|---|
| `name` | body | string | Yes | Not blank, maximum 200 characters |
| `email` | body | string | Yes | Valid email, maximum 200 characters, unique ignoring case |

```json
{
  "name": "Grace Hopper",
  "email": "grace@library.local"
}
```

**Success — 201:** the created member.

**Failure:** 400, 401, 403. 409 `EMAIL_ALREADY_EXISTS` or `DATA_CONFLICT`.

### `GET /api/v1/members/{id}`

A librarian can read any member. A member can read only their own record.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive member id |

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

**Success — 201**

```json
{
  "id": 10,
  "bookId": 1,
  "memberId": 1,
  "borrowedAt": "2026-10-19T01:30:00Z",
  "dueDate": "2026-11-02T01:30:00Z",
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

### `POST /api/v1/loans/{id}/return`

Return a book. A member may return only their own loan. A librarian may return any loan. Returning a loan that is already returned does not change it.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive loan id |

**Request body:** none.

**Success — 200:** the loan, with `returnedAt` set. A second return returns the same loan and still 200.

**Failure:** 400, 401, 403, 404.

### `GET /api/v1/loans`

List loans. A librarian can list every loan or filter by member. A member always receives only their own loans.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `memberId` | query | number | No | Positive member id. A member cannot ask for another member |
| `page` | query | number | No | Page, default 0 |
| `size` | query | number | No | Page size, default 20 |

**Success — 200:** a page of loan objects.

**Failure:** 400, 401. 403 when a member requests another member's loans.

### `GET /api/v1/loans/{id}`

One loan. A librarian can read any loan. A member can read only their own.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `id` | path | number | Yes | Positive loan id |

**Success — 200:** one loan object.

**Failure:** 400, 401, 403, 404.

### `GET /api/v1/borrow-history`

Every borrow and return record. Librarian only.

**Query parameters:** `page`, `size`.

**Success — 200:** a page of loan objects, including returned loans.

**Failure:** 400, 401, 403.

### `GET /api/v1/borrowing-rules`

The rules currently loaded from `application.properties`. Any signed-in user. No parameters.

**Success — 200**

```json
{
  "maxActiveLoans": 5,
  "blockWhenOverdue": true,
  "loanDurationDays": 14
}
```

**Failure:** 401 `UNAUTHORIZED`.

### `GET /actuator/health`

Public. No parameters and no body.

**Success — 200:** status `UP`, with `ping`, `db`, and `diskSpace` components.

**Failure — 503:** status `DOWN` when one of those checks fails.

### `GET /actuator/metrics` and `GET /actuator/metrics/{metricName}`

Any signed-in user.

| Parameter | In | Type | Mandatory | Description |
|---|---|---|---|---|
| `metricName` | path | string | Yes, on the detail call | Metric name, such as `library.loans.borrowed` |
| `tag` | query | string | No | Repeatable `key:value` filter |

**Success — 200:** Actuator metric JSON.

**Failure:** 401. 404 when the metric name does not exist.

## 11. Edge cases

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
