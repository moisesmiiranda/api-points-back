## 1. Environment safety
- [x] 1.1 [BACK] Split Flyway locations: `db/migration` (schema only) and `db/dev` (V2/V5 demo data), loaded only with the `dev` profile. (No cleanup migration was written: nothing had been deployed. If a shared DB already ran V2/V5, remove `admin2@pointsback.local` and the demo users by hand. Note: a DB first started without the dev profile cannot later load V2/V5, because Flyway rejects the out-of-order versions; use a fresh DB for dev.)
- [x] 1.2 [BACK] Profiles. Deviation: there is no `application-prod.yml`; the base `application.yml` is the secure default (no secret defaults) and `application-dev.yml` opts in to dev defaults, so forgetting a profile can never enable demo data. `JWT_SECRET`/`ADMIN_EMAIL`/`ADMIN_PASSWORD` fail fast on missing values; a missing `DB_PASSWORD` fails when the DB rejects the login. CORS origins are left to `mvp-06` 1.1.
- [x] 1.3 [BACK] Startup guard: fail if JWT secret is shorter than 32 bytes or equals the dev default in prod.
- [x] 1.4 [BACK] Add `.env.example`; update README (remove published test passwords from prod instructions).
- [x] 1.5 [BACK] Dockerfile: non-root `USER` and dependency-layer caching done; compose publishes Postgres on 127.0.0.1 only and activates the dev profile. `HEALTHCHECK` is deferred to `mvp-06` 1.2 (needs Actuator).

## 2. Validation and errors
- [x] 2.1 [BACK] Add `spring-boot-starter-validation`; constraints on all DTOs (CPF format/check digits, CNPJ, email, `amount > 0`, `valuePerPoint > 0`, required fields).
- [x] 2.2 [BACK] Extend `GlobalExceptionHandler`: `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `DataIntegrityViolationException` (409 for duplicate CPF/CNPJ/email), catch-all 500 without stack trace.
- [x] 2.3 [BACK] Replace `null` returns with `NotFoundException` (404) in client/purchase/establishment services.
- [x] 2.4 [BACK] `V6__add_constraints_and_indexes.sql`: NOT NULL, CHECK (points >= 0, valuePerPoint > 0, amount > 0) and indexes. `cpf` already has a unique index. `created_at/updated_at` are deferred: `purchase.created_at` is `mvp-03` 1.1 and the client/person timestamps belong to `mvp-02` 1.1.

## 3. Integrity and tenancy
- [x] 3.1 [BACK] `@Transactional` on purchase and points writes, with `SELECT ... FOR UPDATE` (`ClientRepository.findByIdForUpdate`). Verified with 40 concurrent purchases on PostgreSQL: 40x200 and the balance is exactly 400.
- [x] 3.2 [BACK] Reject any operation that would leave points negative (409/422).
- [x] 3.3 [BACK] `updatePurchaseById`: validate that the target client belongs to the caller's establishment; recompute points when amount or client changes.
- [x] 3.4 [BACK] Confirmed: `EstablishmentService.updateEstablishmentById` already rejected STAFF (403). Added HTTP-level tests; no `@PreAuthorize` was needed.
- [x] 3.5 [BACK] Ensure a deactivated user is rejected on the next request (check `active` in the JWT filter or via short-lived cache) instead of waiting for token expiry.

## 4. Auth hardening
- [x] 4.1 [BACK] `LoginRateLimiter`: 5 failures per IP+email (and 20 per email) in 15 min, then 429. In-memory, so per instance. If deployed behind a reverse proxy, the IP seen is the proxy's until forwarded headers are configured (`mvp-06`).
- [x] 4.2 [BACK] Password 8 to 72 characters on create/update user (72 is BCrypt's limit); the bootstrap admin password needs 12+ outside dev.

## 5. Tests
- [x] 5.1 [BACK] HTTP-level (filters enabled) 401/403 tests for `/users`, `/purchases`, `/establishments`, `/clients`.
- [x] 5.2 [BACK] Concurrency test: parallel purchases for the same client keep the balance correct.
- [x] 5.3 [BACK] `./gradlew build` passes (170 tests). Service line coverage: PurchaseService 96%, ClientService 93%, EstablishmentService 100%, UserService 92%, AuthService 100%.
