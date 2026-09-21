## 1. Legal documents
- [ ] 1.1 [LEGAL] Draft Terms of Use, Privacy Policy and DPA (controller/processor); review with a lawyer.
- [ ] 1.2 [LEGAL] Define the notice/consent text a establishment shows consumers (including the group-sharing clause).

## 2. Acceptance
- [ ] 2.1 [BACK] Migration: `terms_acceptance` (user, document, version, accepted_at, ip).
- [ ] 2.2 [BACK] Require acceptance of the current version before using the panel; endpoint to record it.
- [ ] 2.3 [FRONT] Acceptance screen at first login and public pages for Terms and Privacy.

## 3. Data subject rights
- [ ] 3.1 [BACK] `DELETE /clients/{id}`: anonymize the account (keep ledger totals for accounting); delete the `Person` when no accounts remain.
- [ ] 3.2 [BACK] `GET /clients/{id}/export` (JSON) for a data subject request.
- [ ] 3.3 [FRONT] Delete/export actions for OWNER with confirmation modal.

## 4. Audit
- [ ] 4.1 [BACK] `audit_log` (actor, action, entity, entity_id, at) for create/update/delete/export of personal data and logins.
- [ ] 4.2 [BACK] Read endpoint for PLATFORM_ADMIN/OWNER (own tenant only).

## 5. Tests
- [ ] 5.1 [BACK] Anonymization leaves no CPF/email/phone in the database or logs; deletion of one account does not affect the person's other accounts.
