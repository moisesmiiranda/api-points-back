## Why

A `Client` is currently unique per CPF globally and belongs to exactly one establishment, so the same consumer cannot be a customer of two shops. The product decision is: the CPF identifies a person; that person may be registered in many establishments, each with its own balance; establishments in the same group may share customers, or opt out.

## What Changes

- Split identity from account: `person` (unique CPF only) and the existing `client` table as the per-establishment account (person x establishment) that keeps the contact data and the points balance.
- Registering a client by CPF reuses the existing `person`; an establishment only ever sees its own accounts.
- Add `establishment_group` and a per-establishment `share_clients` flag (model + migration now; sharing visibility behavior is SHOULD for the MVP).
- Fast lookup of a client by CPF or phone within the caller's establishment.
- Migrate existing `client` rows without data loss.

## Capabilities

### New Capabilities
- `client-identity`: person/account model, CPF uniqueness, per-establishment isolation, group sharing rules.
### Modified Capabilities
- `authorization`: tenant scoping moves from `Client.establishment_id` to `ClientAccount.establishment_id`.

## Impact

`Client` entity/DTO/service/controller, `Purchase` (references the account), Flyway migrations, all client/purchase tests, frontend client and purchase screens.
Depends on: `mvp-01-secure-foundation`.
