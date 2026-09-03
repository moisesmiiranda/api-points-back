## Context

The API today (`ClientController`, `EstablishmentController`, `PurchaseController`) has no notion of a caller identity — every request is trusted equally. The domain has two existing entities, `Client` (loyalty program member) and `Establishment` (business with a points program), but neither has credentials or a login concept. `Purchase` links a `Client` to an `Establishment` and drives point accrual/redemption.

This change introduces a login-capable identity (`User`) and a stateless JWT-based authentication/authorization layer on top of the existing REST API, built with Spring Security. It is a single-service monolith (no separate auth server, no existing OAuth2/SSO provider to integrate with), so the design favors a self-contained, self-issued JWT approach over federating to an external identity provider.

## Goals / Non-Goals

**Goals:**
- Every existing endpoint requires a valid, non-expired JWT, except the login endpoint itself.
- Three roles (`PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER`, `ESTABLISHMENT_STAFF`) are enforced consistently across all endpoints.
- `ESTABLISHMENT_OWNER` and `ESTABLISHMENT_STAFF` are scoped to exactly one establishment; they cannot read or modify another establishment's clients, purchases, or settings.
- Passwords are never stored or logged in plaintext.
- Authentication/authorization failures produce consistent, predictable HTTP status codes and bodies (`401` vs `403`).

**Non-Goals (this change):**
- Refresh tokens / token revocation / logout blacklisting — access tokens simply expire.
- Password reset / "forgot password" flows.
- Multi-establishment staff (one `User` tied to more than one `Establishment`).
- Client-facing (loyalty member) login — `Client` remains a business record managed by establishment staff, not a login identity, in this change.
- OAuth2/social login or integration with an external identity provider.
- Rate limiting / brute-force login protection (noted as a risk, not solved here).

## Decisions

- **New `User` entity, separate from `Client`.** `Client` represents a loyalty program member (no login); `User` represents a person who authenticates. Conflating them would force every loyalty member to have credentials, which isn't a requirement. `User` has: id, name, email (unique, used as login), hashed password, role (`PLATFORM_ADMIN` | `ESTABLISHMENT_OWNER` | `ESTABLISHMENT_STAFF`), and an optional `establishment` association (null for `PLATFORM_ADMIN`, required for the other two roles).
- **Self-issued stateless JWT, signed with a symmetric key (HS256).** Alternative considered: Spring Authorization Server / OAuth2 resource server pattern — rejected as unnecessary complexity for a single-service API with no third-party clients. The JWT is validated on every request by a custom filter; no server-side session or token store is kept, keeping the API horizontally scalable.
- **Password hashing via `BCryptPasswordEncoder`.** Industry-standard, built into Spring Security, no extra dependency.
- **Token claims carry `userId`, `role`, and `establishmentId`** (the latter omitted/null for `PLATFORM_ADMIN`). Authorization checks read role and establishment scope directly from the validated token rather than re-querying the database on every request, keeping request-time checks cheap.
- **Authorization enforced at two levels:**
  1. Role-based, method-level checks (e.g., only `PLATFORM_ADMIN` and `ESTABLISHMENT_OWNER` may create an `Establishment`).
  2. Ownership checks in the service layer (e.g., `ESTABLISHMENT_STAFF`/`ESTABLISHMENT_OWNER` may only act on `Client`/`Purchase` records belonging to their own `establishmentId` claim).
  Role checks alone are insufficient because two establishments both have staff with the same role — the distinguishing factor is establishment ownership, not just role name.
- **Account provisioning hierarchy:**
  - `PLATFORM_ADMIN` can create/manage accounts of any role.
  - `ESTABLISHMENT_OWNER` can create/manage `ESTABLISHMENT_STAFF` accounts, scoped to their own establishment only.
  - `ESTABLISHMENT_STAFF` cannot create or manage any accounts.
- **Centralized security error handling.** A single `AuthenticationEntryPoint` returns `401` for missing/invalid/expired tokens; a single `AccessDeniedHandler` returns `403` for authenticated-but-unauthorized actions. Both return a consistent JSON error shape, avoiding Spring Security's default HTML error pages.
- **Login endpoint (`POST /auth/login`) is the only unauthenticated endpoint.** All others are denied by default (`anyRequest().authenticated()`), rather than allow-listed, so newly added endpoints are secure by default.
- **`Client` gains an `establishment` association.** Today `Client` has no link to an `Establishment` — only `Purchase` connects the two. Establishment-scoped authorization (an owner/staff member only seeing their own establishment's clients) requires `Client` to belong to an establishment directly; inferring scope transitively through purchase history is unreliable (a client with no purchases yet would be unscoped/unreachable by anyone but a platform admin). This is a small, additive data model change alongside the security work.

## Risks / Trade-offs

- **[Risk] Symmetric JWT signing key must be kept secret and out of source control.** → Mitigation: key is externalized via environment variable / secrets manager, never committed in `application.yml` with a real value; document required env var in the spec/tasks.
- **[Risk] No token revocation means a stolen/leaked token remains valid until expiry.** → Mitigation: keep access token expiration short (spec will define an exact value); explicitly documented as a non-goal/follow-up (refresh + revocation) rather than silently ignored.
- **[Risk] Ownership checks living in the service layer (not purely declarative) are easy to forget on new endpoints.** → Mitigation: acceptance criteria in the specs must explicitly enumerate the ownership rule per endpoint, and tests must cover cross-establishment access attempts for every mutating endpoint.
- **[Risk] Breaking change** — existing integrations calling these endpoints anonymously will start failing with `401`. → Mitigation: called out explicitly in the proposal as **BREAKING**; no silent fallback to anonymous access is provided.
- **[Trade-off] No brute-force/lockout protection in this change** increases exposure to credential-stuffing attacks. → Accepted as out of scope for this change; flagged as an open question for a fast-follow.

## Migration Plan

1. Add Spring Security + JWT library dependencies; add a Flyway migration creating the `users` table (id, name, email unique, password_hash, role, establishment_id nullable FK).
2. Implement password hashing, `User` repository/service, and the `/auth/login` endpoint (issues JWT on valid credentials).
3. Implement the JWT filter and Spring Security filter chain configured deny-by-default, with the login endpoint allow-listed.
4. Apply role- and ownership-based rules to `ClientController`, `EstablishmentController`, `PurchaseController` per the authorization spec.
5. Seed at least one `PLATFORM_ADMIN` account via migration/data seed so the system is bootstrappable after deployment (otherwise no one can create the first account).
6. Update existing controller/integration tests to authenticate as the appropriate role; add new tests for 401/403 and cross-establishment access cases.

Rollback: this change has not shipped to any consumer yet (pre-release API), so rollback is simply reverting the branch/PR; no production data migration reversal is required beyond the standard Flyway `users` table addition being dropped in a down-migration if the team uses one.

## Open Questions

- Exact JWT expiration duration (e.g., 15 min, 1 hour, 24 hours) — to be fixed as a concrete acceptance criterion in the `authentication` spec.
- Should the initial `PLATFORM_ADMIN` bootstrap account be created via a Flyway data migration (fixed credentials to rotate on first login) or an out-of-band ops process? Assumed: Flyway seed with a mandatory first-login password change is out of scope; a documented seed account is used for now.
- Is `ESTABLISHMENT_OWNER` allowed to remove/deactivate their own `ESTABLISHMENT_STAFF` accounts, and can they deactivate themselves? Assumed yes for staff, no for self-deactivation (only `PLATFORM_ADMIN` can deactivate an owner) — to be confirmed in the `authorization` spec.
