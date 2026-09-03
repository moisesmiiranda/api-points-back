## ADDED Requirements

### Requirement: User Credential Storage
The system SHALL store a `User` account for every person who can log in, with a unique email address (used as the login identifier), a securely hashed password, a role, and — for non-`PLATFORM_ADMIN` roles — an association to exactly one `Establishment`. The system SHALL NOT store passwords in plaintext or in a reversible format.

#### Scenario: Password is hashed at rest
- **WHEN** a `User` account is created or its password is changed
- **THEN** the system stores only a salted hash of the password, never the plaintext value

#### Scenario: Duplicate email is rejected
- **WHEN** a new `User` is created with an email address that already belongs to another `User`
- **THEN** the system rejects the request and does not create a duplicate account

#### Scenario: Non-admin account requires an establishment
- **WHEN** a `User` is created with role `ESTABLISHMENT_OWNER` or `ESTABLISHMENT_STAFF` without an associated `Establishment`
- **THEN** the system rejects the request

### Requirement: Password Confidentiality in Responses
The system SHALL NOT include a user's password or password hash in any API response body or application log.

#### Scenario: User record omits password
- **WHEN** any endpoint returns a `User` (e.g., "get current user" or an admin listing accounts)
- **THEN** the response body does not contain the password or password hash field

### Requirement: Login and Token Issuance
The system SHALL expose a login endpoint that accepts an email and password and, on successful verification, returns a signed JSON Web Token (JWT). The login endpoint SHALL be the only endpoint reachable without a valid token.

#### Scenario: Successful login returns a token
- **GIVEN** a `User` account exists with email `owner@shop.com` and a known password
- **WHEN** a login request is made with the correct email and password
- **THEN** the system responds `200 OK` with a signed JWT containing the user's id, role, and establishment id (if any)

#### Scenario: Login with incorrect password is rejected
- **GIVEN** a `User` account exists with email `owner@shop.com`
- **WHEN** a login request is made with that email and an incorrect password
- **THEN** the system responds `401 Unauthorized` and does not issue a token

#### Scenario: Login with unknown email is rejected
- **WHEN** a login request is made with an email that has no matching `User` account
- **THEN** the system responds `401 Unauthorized` with the same generic error used for a wrong password, without revealing whether the email exists

### Requirement: Token Claims and Expiration
Every issued JWT SHALL encode the user's id, role, and establishment id (when applicable), be signed with a server-held secret key, and carry a fixed, finite expiration time. The system SHALL reject any token past its expiration.

#### Scenario: Token has an expiration claim
- **WHEN** a JWT is issued at login
- **THEN** the token includes an expiration timestamp no more than the system's configured maximum token lifetime from issuance

#### Scenario: Expired token is rejected
- **GIVEN** a JWT whose expiration timestamp is in the past
- **WHEN** that token is presented on any protected endpoint
- **THEN** the system responds `401 Unauthorized` and does not process the request

### Requirement: Protected Endpoint Token Validation
Every endpoint except login SHALL require a valid `Authorization: Bearer <token>` header. The system SHALL validate the token's signature and expiration before allowing the request to reach business logic.

#### Scenario: Missing token is rejected
- **WHEN** a request is made to any protected endpoint without an `Authorization` header
- **THEN** the system responds `401 Unauthorized`

#### Scenario: Malformed or tampered token is rejected
- **WHEN** a request is made with an `Authorization` header whose token fails signature verification (e.g., altered payload, wrong signing key, garbage value)
- **THEN** the system responds `401 Unauthorized`

#### Scenario: Valid token grants access to the authentication layer
- **GIVEN** a `User` has a valid, unexpired JWT issued at login
- **WHEN** that token is presented on a protected endpoint
- **THEN** the request is authenticated and passed on to authorization checks (role/establishment rules apply separately)
