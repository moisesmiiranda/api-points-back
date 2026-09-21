## Why

The API cannot be exposed to a paying customer as-is: seed migrations create a known-password PLATFORM_ADMIN in every environment, secrets have insecure defaults, input is not validated (bad input yields 500s and a divide-by-zero), and purchase/points writes are not transactional. This change makes the backend safe to deploy and is a prerequisite for every other MVP change.

## What Changes

- Move demo data (`V2`, `V5`) out of production migrations (dev-only profile/location); remove the `admin2@pointsback.local` backdoor from any prod path.
- Require `JWT_SECRET`, `ADMIN_PASSWORD`, `DB_PASSWORD` in the `prod` profile with fail-fast at startup; add `.env.example`.
- Fix cross-tenant leak in `PurchaseService.updatePurchaseById`.
- Add Bean Validation and complete `GlobalExceptionHandler` (validation, malformed JSON, duplicate CPF/CNPJ, generic 500).
- Make `registerPurchase` / points changes transactional and concurrency-safe; balance can never go negative.
- Return 404 instead of `null`/200 for missing resources (also for PLATFORM_ADMIN).
- Verify staff cannot `PUT /establishments/{id}`; add HTTP-level authorization tests for `/users`, `/purchases`, `/establishments`.
- Basic login rate limiting and minimum password policy.

## Capabilities

### New Capabilities
- `input-validation`: validation rules and error contract.
### Modified Capabilities
- `authorization`: cross-tenant purchase update, establishment update by staff.

## Impact

`PurchaseService`, `ClientService`, `EstablishmentController`, `GlobalExceptionHandler`, `application.yml` (+ `application-dev.yml`, `application-prod.yml`), Flyway locations, `build.gradle.kts` (validation starter), tests.
