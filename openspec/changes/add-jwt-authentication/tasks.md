## 1. Dependencies & Data Model

- [x] 1.1 Add Spring Security and a JWT library (e.g. `io.jsonwebtoken:jjwt`) to `build.gradle.kts`.
- [x] 1.2 Add a Flyway migration creating the `users` table: id, name, email (unique), password_hash, role, establishment_id (nullable FK to `establishments`).
- [x] 1.3 Add a Flyway migration adding an `establishment_id` (FK to `establishments`, required) column to `clients`, per the scoping decision in `design.md`.
- [x] 1.4 Add a Flyway seed migration or documented bootstrap process for the first `PLATFORM_ADMIN` account. (Implemented as `AdminBootstrapRunner`, an `ApplicationRunner` that creates the account from `ADMIN_EMAIL`/`ADMIN_PASSWORD` on first startup, rather than a hardcoded SQL seed.)
- [x] 1.5 Create the `User` JPA entity, role enum (`PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER`, `ESTABLISHMENT_STAFF`), and `UserRepository`.
- [x] 1.6 Add the `establishment` association to the `Client` entity and update `ClientDto`/mappers accordingly.

## 2. Authentication

- [x] 2.1 Add `PasswordEncoder` (BCrypt) bean and use it wherever a `User` password is set.
- [x] 2.2 Implement credential lookup/verification backed by `UserRepository`. (Implemented directly in `AuthService` rather than via Spring's `UserDetailsService` SPI, since login is handled without an `AuthenticationManager`.)
- [x] 2.3 Implement JWT issuance utility: signs a token with claims `userId`, `role`, `establishmentId` (nullable), `iat`, `exp`, using an externally-configured secret key and expiration duration.
- [x] 2.4 Implement JWT parsing/validation utility: verifies signature and expiration, extracts claims.
- [x] 2.5 Add `POST /auth/login` endpoint: validates credentials, returns the signed JWT on success, generic `401` on failure (per `authentication` spec's "same generic error" scenario).
- [x] 2.6 Ensure `User` responses (e.g. "current user") never serialize the password hash.

## 3. Security Filter Chain

- [x] 3.1 Implement a JWT authentication filter (`OncePerRequestFilter`) that reads the `Authorization: Bearer` header, validates the token, and populates the `SecurityContext` with the user's id, role, and establishment id.
- [x] 3.2 Configure `SecurityFilterChain`: deny-by-default (`anyRequest().authenticated()`), allow-list only `POST /auth/login`, stateless session policy, register the JWT filter.
- [x] 3.3 Implement a custom `AuthenticationEntryPoint` returning a consistent `401` JSON body for missing/invalid/expired tokens.
- [x] 3.4 Implement a custom `AccessDeniedHandler` returning a consistent `403` JSON body for authenticated-but-forbidden requests.
- [x] 3.5 Externalize the JWT signing secret and expiration duration via `application.yml` + environment variable (no real secret committed).

## 4. Authorization Rules — Accounts

- [x] 4.1 Add `User` management endpoints (create/update/deactivate) with role checks: `PLATFORM_ADMIN` unrestricted; `ESTABLISHMENT_OWNER` limited to creating/updating `ESTABLISHMENT_STAFF` within their own establishment; `ESTABLISHMENT_STAFF` forbidden from all account management.
- [x] 4.2 Enforce that an `ESTABLISHMENT_OWNER` cannot change their own role or establishment association.

## 5. Authorization Rules — Establishments

- [x] 5.1 Restrict `EstablishmentController` create endpoint to `PLATFORM_ADMIN`.
- [x] 5.2 Restrict `EstablishmentController` get/update endpoints so `ESTABLISHMENT_OWNER`/`ESTABLISHMENT_STAFF` can only target their own establishment id (owner: read+update, staff: read only); `PLATFORM_ADMIN` unrestricted.

## 6. Authorization Rules — Clients

- [x] 6.1 Update `ClientController` create/get/update/points endpoints to require the caller's establishment (from the token) to match the client's `establishment_id`, except for `PLATFORM_ADMIN`.
- [x] 6.2 Ensure `GET /clients/all` is scoped to the caller's establishment for `ESTABLISHMENT_OWNER`/`ESTABLISHMENT_STAFF`, and unscoped for `PLATFORM_ADMIN`.
- [x] 6.3 Ensure a client belonging to a different establishment produces the same `403`/`404` response as a nonexistent client (no existence leakage), per the `authorization` spec.

## 7. Authorization Rules — Purchases

- [x] 7.1 Update `PurchaseController` create endpoint to require the purchase's `establishment_id` to match the caller's establishment, except for `PLATFORM_ADMIN`.
- [x] 7.2 Scope `GET /purchases` (list) and `GET /purchases/{id}` to the caller's own establishment for `ESTABLISHMENT_OWNER`/`ESTABLISHMENT_STAFF`; unscoped for `PLATFORM_ADMIN`.
- [x] 7.3 Apply the same establishment-match check to `PUT /purchases/{id}`.

## 8. Testing

- [x] 8.1 Unit tests for JWT issuance/validation (valid token, expired token, tampered signature).
- [x] 8.2 Unit tests for password hashing and login success/failure paths.
- [x] 8.3 Integration tests covering `401` for missing/invalid token (covered end-to-end for a representative protected endpoint in `AuthenticationIntegrationTest`; expired-token handling is covered at the `JwtService` unit level).
- [x] 8.4 Tests covering `403` for cross-establishment access on `ClientService`, `EstablishmentService`, and `PurchaseService` (owner and staff roles), at the service-unit level.
- [x] 8.5 Tests covering `PLATFORM_ADMIN` unrestricted cross-establishment access.
- [x] 8.6 Tests covering account provisioning permission rules (owner creating staff, owner blocked from creating admin/owner or staff elsewhere, staff blocked from creating anyone).
- [x] 8.7 Updated existing controller/service tests for the new `ClientDto` field and establishment-scoping; `@WebMvcTest` slices that mock the service layer disable the security filter chain (`addFilters = false`) since they only assert controller↔service wiring.

## 9. Documentation

- [x] 9.1 Document required environment variables (JWT secret, expiration, admin bootstrap) in the README.
- [x] 9.2 Document the account bootstrap process for the first `PLATFORM_ADMIN`.
