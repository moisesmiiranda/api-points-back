## 1. Data model
- [x] 1.1 [BACK] `V7`: `person` holds identity only (id, cpf UNIQUE, digits only, created_at). Two deliberate deviations: (a) name/email/phone stay on each account, so an establishment never sees data another one recorded for the same CPF; (b) the existing `client` table is the account table (no `client_account` rename), with `person_id`, `created_at`, `updated_at` and UNIQUE(person_id, establishment_id). Keeping the table keeps ids, so purchases and the frontend are untouched.
- [x] 1.2 [BACK] `V7` builds `person` from the distinct normalized CPFs, links every client and drops `client.cpf`. No repointing was needed (ids are kept). Verified on H2 (`ClientIdentityMigrationTest`) and on PostgreSQL with legacy data: counts, balances, contact data and purchase links preserved. The migration fails on purpose if a client has no CPF or two clients of one establishment share a CPF after normalization.
- [x] 1.3 [BACK] Migration: `establishment_group` and `establishment.group_id`, `establishment.share_clients` (default false).
- [x] 1.4 [BACK] JPA entities, repositories and mappers for `Person`, `ClientAccount`, `EstablishmentGroup`.

## 2. Behavior
- [x] 2.1 [BACK] `POST /clients`: find-or-create `Person` by CPF, create the `ClientAccount` for the caller's establishment; 409 if the account already exists.
- [x] 2.2 [BACK] Rule: contact data is per account, so editing never affects another establishment. Changing an account's CPF re-links it to another person (found or created) and is refused with 409 if that person already has an account there.
- [x] 2.3 [BACK] `GET /clients/search?cpf=&phone=` scoped to the caller's establishment.
- [x] 2.4 [BACK] Isolation: every client/purchase query filters by `client_account.establishment_id`; a staff of A never sees balance or data from B.
- [x] 2.5 [BACK] `POST /clients/import`: needs the caller's establishment and at least one other group member with `share_clients`; copies contact data only, balance starts at 0. Unknown CPF, non-sharing source and a person outside the group all return the same 404.
- [x] 2.6 [BACK] `POST/GET /establishment-groups` and `PUT /establishments/{id}/group` (PLATFORM_ADMIN); `GET/PUT /establishments/{id}/sharing` (owner or admin; staff cannot change it). Leaving a group turns sharing off. Sharing can only be enabled inside a group.

## 3. Frontend
- [x] 3.1 [FRONT] Client register form. Adapted for privacy: no prefill from other establishments. On leaving the CPF field it checks the caller's own establishment and warns (with a link) if the CPF is already registered; when the establishment shares clients it offers "Importar do grupo". CPF is validated with check digits. NOT type-checked or run: Node/npm are not installed on this machine, so run `npm run build` and `npm run lint` before merging.
- [x] 3.2 [FRONT] The purchase screen and the client list already had a search box; they now ignore punctuation ("52998224725" finds "529.982.247-25", phone likewise). `clientService.search` calls `GET /clients/search`. Same caveat: not built or run.

## 4. Tests
- [x] 4.1 [BACK] Same CPF in two establishments yields two independent balances.
- [x] 4.2 [BACK] Cross-tenant tests (client search, get, purchase) return 403/404.
- [x] 4.3 [BACK] `ClientIdentityMigrationTest` (Flyway to V6, legacy rows, then V7).
