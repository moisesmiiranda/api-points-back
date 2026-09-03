## Why

The API currently has no authentication or authorization layer — every endpoint (client, establishment, and purchase management) is open to anonymous callers. Any client can read or mutate any establishment's data, award or redeem points, or view another establishment's clients. Before this API can be used by real establishments and staff, it needs to verify who is calling and restrict what they can do based on their role.

## What Changes

- Introduce a `User` (account/credential) domain, separate from the existing `Client` and `Establishment` business entities, to represent people who can log into the system.
- Add three roles: `PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER`, `ESTABLISHMENT_STAFF`.
- Add a login endpoint that exchanges valid credentials for a signed JWT access token.
- Add stateless JWT-based request authentication (no server-side session) via Spring Security.
- Add role- and ownership-based authorization rules to all existing endpoints (`ClientController`, `EstablishmentController`, `PurchaseController`), replacing today's fully-open access. **BREAKING**: all existing endpoints will start requiring a valid `Authorization: Bearer <token>` header; unauthenticated calls that previously succeeded will now return `401`.
- Add account lifecycle rules: password storage (hashed, never plaintext), account creation/association with an establishment, and token expiration.
- Add centralized security error responses (`401` for missing/invalid/expired tokens, `403` for authenticated-but-forbidden actions).

## Capabilities

### New Capabilities
- `authentication`: Login, credential validation, JWT issuance/expiration/refresh rules, password storage rules, and account lockout/error behavior.
- `authorization`: Role definitions (`PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER`, `ESTABLISHMENT_STAFF`), per-role and per-establishment access rules, and how those rules apply to client, establishment, and purchase operations.

### Modified Capabilities
- None. No existing specs are defined for the current endpoints yet, so this change establishes the baseline authorization behavior for them directly within the `authorization` capability rather than as a delta to a prior spec.

## Impact

- **Affected code**: `ClientController`, `EstablishmentController`, `PurchaseController` (all endpoints gain access rules); new `User` model, repository, service, and `AuthController`; new Spring Security configuration and JWT filter/utility classes; `application.yml` (JWT secret/expiration configuration); a new Flyway migration adding a `users` table (and any role/establishment-association columns).
- **Dependencies**: adds Spring Security and a JWT library to `build.gradle.kts`.
- **Existing data**: `Establishment` and `Client` entities are unaffected in shape; `Establishment` gains an association to the `User` account(s) that own/staff it.
- **Clients of the API**: any existing consumer of these endpoints must now authenticate; this is a breaking change to the API's access model.
