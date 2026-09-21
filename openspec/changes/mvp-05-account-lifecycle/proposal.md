## Why

There is no forgot-password, no self password change and no email sending; every lockout becomes a manual support ticket. There is also no way to suspend an establishment that stops paying, and billing is manual for the first customer.

## What Changes

- Transactional email (provider TBD, free tier) with a port/adapter so the provider can change.
- Forgot/reset password via single-use, expiring token; change own password.
- Optional forced password change on first login for accounts created by an admin/owner.
- `establishment.status` (`TRIAL` | `ACTIVE` | `SUSPENDED`) and `plan`, `trial_ends_at`; suspended establishments are blocked from writes/login.
- Documented concierge onboarding procedure.

## Capabilities

### New Capabilities
- `password-recovery`, `establishment-status`
### Modified Capabilities
- `authentication`: reset/change password, first-login change, blocked login for suspended tenants.

## Impact

`AuthService`, `UserService`, new email module, `Establishment`, JWT filter, Flyway migrations, login/settings screens.
Depends on: `mvp-01-secure-foundation`.
