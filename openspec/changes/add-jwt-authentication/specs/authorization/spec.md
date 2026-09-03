## ADDED Requirements

### Requirement: Defined Roles and Establishment Scope
The system SHALL support exactly three roles: `PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER`, and `ESTABLISHMENT_STAFF`. `PLATFORM_ADMIN` accounts SHALL NOT be tied to any single establishment and SHALL be able to act across all establishments. `ESTABLISHMENT_OWNER` and `ESTABLISHMENT_STAFF` accounts SHALL each be tied to exactly one establishment and SHALL be scoped to that establishment's data only.

#### Scenario: Platform admin is not establishment-scoped
- **GIVEN** a `User` with role `PLATFORM_ADMIN`
- **WHEN** that user authenticates
- **THEN** their token carries no establishment restriction and they are permitted to act on any establishment's data, subject to the other rules in this spec

#### Scenario: Establishment owner is scoped to one establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** that user authenticates
- **THEN** their token identifies establishment `A`, and every subsequent authorization check is evaluated against establishment `A`

### Requirement: Account Provisioning Permissions
The system SHALL restrict who may list, create, update, or deactivate `User` accounts based on role:
- `PLATFORM_ADMIN` MAY list, create, update, or deactivate any account of any role.
- `ESTABLISHMENT_OWNER` MAY list, create, update, or deactivate `ESTABLISHMENT_STAFF` accounts belonging to their own establishment only. When an `ESTABLISHMENT_OWNER` lists accounts, the response SHALL contain only the `ESTABLISHMENT_STAFF` of their own establishment.
- `ESTABLISHMENT_STAFF` SHALL NOT list, create, update, or deactivate any account.
- `ESTABLISHMENT_OWNER` SHALL NOT create, update, or deactivate `PLATFORM_ADMIN` or `ESTABLISHMENT_OWNER` accounts, and SHALL NOT change any account's role or establishment assignment (including their own).

#### Scenario: Owner lists only their own establishment's staff
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they request the list of `User` accounts
- **THEN** the response contains only `ESTABLISHMENT_STAFF` accounts of establishment `A`, and no `PLATFORM_ADMIN` or `ESTABLISHMENT_OWNER` accounts

#### Scenario: Staff cannot list accounts
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF`
- **WHEN** they request the list of `User` accounts
- **THEN** the system responds `403 Forbidden`

#### Scenario: Owner manages their own staff
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they update or deactivate an `ESTABLISHMENT_STAFF` account of establishment `A`
- **THEN** the change is applied successfully

#### Scenario: Owner cannot manage accounts outside their scope
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they attempt to update or deactivate a `PLATFORM_ADMIN`, an `ESTABLISHMENT_OWNER`, or any account outside establishment `A`
- **THEN** the system responds `403 Forbidden`

#### Scenario: Owner creates staff for their own establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they create a new `ESTABLISHMENT_STAFF` account associated with establishment `A`
- **THEN** the account is created successfully

#### Scenario: Owner cannot create staff for another establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they attempt to create an `ESTABLISHMENT_STAFF` account associated with establishment `B`
- **THEN** the system responds `403 Forbidden` and no account is created

#### Scenario: Staff cannot create accounts
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF`
- **WHEN** they attempt to create any `User` account
- **THEN** the system responds `403 Forbidden`

#### Scenario: Owner cannot promote themselves to admin
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER`
- **WHEN** they attempt to update their own account's role to `PLATFORM_ADMIN`
- **THEN** the system responds `403 Forbidden` and the role is unchanged

### Requirement: Establishment Management Access
Only `PLATFORM_ADMIN` accounts SHALL be able to create new establishments. An establishment's own `ESTABLISHMENT_OWNER` MAY view and update that establishment's own details. `ESTABLISHMENT_STAFF` MAY view (but not update) their own establishment's details. No `ESTABLISHMENT_OWNER` or `ESTABLISHMENT_STAFF` SHALL be able to view or modify a different establishment's details.

#### Scenario: Platform admin creates an establishment
- **GIVEN** a `User` with role `PLATFORM_ADMIN`
- **WHEN** they submit a request to create a new establishment
- **THEN** the establishment is created successfully

#### Scenario: Establishment owner cannot create establishments
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER`
- **WHEN** they submit a request to create a new establishment
- **THEN** the system responds `403 Forbidden`

#### Scenario: Owner updates their own establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they submit an update to establishment `A`'s details
- **THEN** the update is applied successfully

#### Scenario: Owner cannot view or update another establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`
- **WHEN** they request to view or update establishment `B`
- **THEN** the system responds `403 Forbidden`

#### Scenario: Staff can view but not update their establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`
- **WHEN** they request to view establishment `A`'s details
- **THEN** the request succeeds
- **WHEN** they attempt to update establishment `A`'s details
- **THEN** the system responds `403 Forbidden`

### Requirement: Client Management Access
`ESTABLISHMENT_OWNER` and `ESTABLISHMENT_STAFF` accounts MAY create, view, and update `Client` records associated with their own establishment only. `PLATFORM_ADMIN` accounts MAY create, view, and update `Client` records for any establishment. A request that references a `Client` belonging to a different establishment than the caller's SHALL be rejected, even if the `Client` id exists.

#### Scenario: Staff manages a client in their own establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`
- **WHEN** they create, view, or update a `Client` associated with establishment `A`
- **THEN** the request succeeds

#### Scenario: Staff cannot access a client from another establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`, and a `Client` `C` associated with establishment `B`
- **WHEN** they attempt to view, update, or award/redeem points for `Client` `C`
- **THEN** the system responds `403 Forbidden`, regardless of whether `Client` `C` exists

#### Scenario: Platform admin manages any client
- **GIVEN** a `User` with role `PLATFORM_ADMIN`
- **WHEN** they view or update a `Client` associated with any establishment
- **THEN** the request succeeds

### Requirement: Purchase Management Access
`ESTABLISHMENT_OWNER` and `ESTABLISHMENT_STAFF` accounts MAY create, view, and update `Purchase` records associated with their own establishment only. `PLATFORM_ADMIN` accounts MAY create, view, and update `Purchase` records for any establishment. Listing purchases SHALL only return purchases belonging to the caller's own establishment, unless the caller is `PLATFORM_ADMIN`.

#### Scenario: Staff records a purchase for their own establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`
- **WHEN** they submit a new `Purchase` for a client, tied to establishment `A`
- **THEN** the purchase is created successfully

#### Scenario: Staff cannot record a purchase for another establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`
- **WHEN** they submit a new `Purchase` tied to establishment `B`
- **THEN** the system responds `403 Forbidden` and no purchase is created

#### Scenario: Purchase listing is scoped to the caller's establishment
- **GIVEN** a `User` with role `ESTABLISHMENT_OWNER` associated with establishment `A`, and purchases exist for both establishment `A` and establishment `B`
- **WHEN** they request the list of purchases
- **THEN** the response contains only purchases belonging to establishment `A`

#### Scenario: Platform admin lists purchases across establishments
- **GIVEN** a `User` with role `PLATFORM_ADMIN`
- **WHEN** they request the list of purchases without an establishment filter
- **THEN** the response contains purchases across all establishments

### Requirement: Cross-Establishment Data Isolation
Regardless of the specific resource, the system SHALL treat any attempt by an `ESTABLISHMENT_OWNER` or `ESTABLISHMENT_STAFF` to read or write data belonging to a different establishment as a forbidden action, and SHALL NOT leak the existence of another establishment's data through error messages (e.g., a `403` SHALL NOT reveal whether the referenced resource id exists).

#### Scenario: Forbidden response does not confirm resource existence
- **GIVEN** a `User` with role `ESTABLISHMENT_STAFF` associated with establishment `A`
- **WHEN** they request a `Client`, `Purchase`, or `Establishment` resource id that either belongs to another establishment or does not exist at all
- **THEN** both cases produce the same `403 Forbidden` (or `404`, chosen consistently) response, without distinguishing "exists but forbidden" from "does not exist"

### Requirement: Authorization Failure Response
The system SHALL respond `403 Forbidden` with a consistent error body when an authenticated caller attempts an action their role or establishment scope does not permit. This SHALL be distinct from the `401 Unauthorized` response used for missing/invalid authentication.

#### Scenario: Authenticated but unauthorized request
- **GIVEN** a `User` with a valid, unexpired token
- **WHEN** they call an endpoint or perform an action outside their role/establishment permissions
- **THEN** the system responds `403 Forbidden` with an error body identifying the request as forbidden (not an authentication failure)
