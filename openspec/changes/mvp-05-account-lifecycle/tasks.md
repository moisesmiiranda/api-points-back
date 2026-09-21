## 1. Email
- [x] 1.1 [BACK] `EmailSender` port with two adapters: `SmtpEmailSender` (Spring Mail; works with any provider that offers SMTP: SES, Resend, Brevo, Mailgun...) and `LoggingEmailSender` (dev/test only, writes the email and its links to the log). `EmailService` sends asynchronously and swallows failures, so callers never wait for the mail server nor see its errors. `StartupMailGuard` refuses to start outside dev/test without `MAIL_HOST`, `MAIL_FROM` and `APP_BASE_URL` (and refuses the log sender, which would put live links in the log). Free-tier limits of the providers were not checked: confirm the current terms before choosing one.
- [x] 1.2 [BACK] `EmailTemplates` (pt-BR, text + HTML, user-supplied values HTML-escaped): password reset, invitation with link, and welcome for a temporary password (which never contains the password).

## 2. Password flows
- [x] 2.1 [BACK] `V9`: `password_reset_token` (only the SHA-256 hash of the token is stored; type RESET or INVITE; `expires_at`, `used_at`), plus `users.must_change_password` and `users.password_changed_at`. Verified on H2 and on PostgreSQL from V8 with legacy data (existing users free, existing establishments ACTIVE).
- [x] 2.2 [BACK] `POST /auth/forgot-password` always answers 200 with the same text (nothing is sent for unknown or deactivated accounts) and is throttled to 5 requests per 15 min per email+IP (429), counting every request so the throttle leaks nothing. `POST /auth/reset-password`: 256-bit random single-use link, 60 min; unknown, expired and used links give the same 400; a new link cancels the older ones; success ends every session the user had.
- [x] 2.3 [BACK] `POST /users/me/password` (current + new, 8-72 chars, new must differ). Returns a fresh token because the change ends the older sessions (the JWT filter now rejects tokens issued before `password_changed_at`). Wrong current passwords count against the login throttle. Deliberately 400, not 401, so the frontend does not read it as an expired session.
- [x] 2.4 [BACK] A user created WITH a password (or whose password a manager changed) gets `must_change_password`: `AccessGate` in the JWT filter answers 403 to everything except `GET /users/me` and `POST /users/me/password`. Created WITHOUT a password, the person is invited by email (72 h link) and needs no forced change. `LoginResponseDto` and `UserDto` carry `mustChangePassword`.
- [x] 2.5 [FRONT] `/esqueci-senha`, `/redefinir-senha?token=` (also the invitation link), `/conta/senha` (voluntary and forced), a "Esqueci minha senha" link on the login, a "Conta > Alterar senha" menu item, and a router guard that sends users with a temporary password to the change screen from any page. The user form now defaults to "Enviar convite por e-mail" and only asks for a temporary password when unchecked.

## 3. Establishment status
- [x] 3.1 [BACK] `V9`: `establishment.status` (existing rows ACTIVE), `plan` (free-text label), `trial_ends_at`. New establishments start as TRIAL with 14 days (`Establishment.DEFAULT_TRIAL_DAYS`).
- [x] 3.2 [BACK] Lazy trial expiry (no scheduler): a TRIAL past `trial_ends_at` has `effectiveStatus` SUSPENDED and is enforced everywhere. Suspended establishments: login answers 402 (after the password is verified, so a wrong password still gets the generic 401), and open sessions get 402 on every call except `GET /users/me` and their own plan, checked against the database on each request, so it takes effect immediately. PLATFORM_ADMIN is never blocked. 402 was chosen (not 403) so the frontend can tell it apart from a permission error.
- [x] 3.3 [BACK] `GET/PUT /establishments/{id}/plan` (PUT only for PLATFORM_ADMIN; the owner reads only their own). The establishment list JSON now also carries `status`, `effectiveStatus`, `plan`, `trialEndsAt`. Screen: Listagens > Estabelecimentos > Plano (with "Suspender agora" / "Reativar agora").
- [x] 3.4 [FRONT] Status badge in the establishment list; the plan screen; a full-screen "Acesso suspenso" notice (from the plan on load, or from a 402 during use); a banner in the last 7 days of a trial.

## 4. Onboarding
- [x] 4.1 [BACK] `docs/onboarding-concierge.md`: create the establishment, invite the owner, first access, reward settings, checklist, manual billing routine and troubleshooting.
- [x] 4.2 [BACK] Creating a user without a password sends the invitation email with the set-password link (72 h).

## 5. Tests
- [x] 5.1 [BACK] Unit tests for the tokens (hash stored, single use, expiry, newest link only, no enumeration, throttle), the access gate, the plan rules, the mail guard, senders and templates; `AccountLifecycleIntegrationTest` (13 HTTP scenarios: reset, throttle, expired link, invite, forced change, session cut-off after a password change, suspension, ended trial, permissions); migration test. Also run on PostgreSQL with a real SMTP server (Mailpit) and, for the UI, in Chromium and WebKit.

## Deviations and follow-ups
- Invitations reuse the reset mechanism (token type INVITE), so there is one code path for "set a password from a link".
- To resend an invitation, use "Esqueci minha senha" (there is no dedicated "resend" button yet).
- Sessions are compared with the password change at one-second precision (like the JWT `iat`), so a token issued in the same second as the change survives it.
- The email address is matched exactly as stored (no case folding), like the login.
- Email delivery failures are only logged (by design, so responses do not depend on the mail server); monitoring should watch for `Failed to send email` (see `mvp-06`).
