## 1. Ledger and purchase date
- [x] 1.1 [BACK] `V8`: `purchase.created_at`, `discount_amount`, `points_redeemed`, `points_earned` (kept so an edit or cancellation reverses exactly what was granted) and `cancelled_at`; `points_ledger` (append-only, with `balance_after`, keyed by `client_id`). CREDIT is backfilled per existing purchase with the running balance, plus an explicit ADJUST for any client whose stored balance disagrees (the seeded demo clients do). Verified on H2 (`PointsLedgerMigrationTest`) and on PostgreSQL from V7 with legacy data. Points earned are now floor(amount / valuePerPoint), matching the documented rule (the old HALF_DOWN at 2 decimals could round 19.996 up to 20).
- [x] 1.2 [BACK] `PointsService.apply` is the only writer of the balance: it requires an active transaction (the caller's row lock) and refuses a negative result.
- [x] 1.3 [BACK] Register writes REDEEM (if points were used) and CREDIT. Edit writes CREDIT then REVERSAL (credit first, so a same-client correction is judged on its net effect). Cancel (`POST /purchases/{id}/cancel`, owner/admin only) returns redeemed points first, then removes earned ones, and is refused with 422 if those were already spent. Purchases paid with points, or already cancelled, cannot be edited (409): cancel and register again.
- [x] 1.4 [BACK] Done. Also: creating a client always starts at 0 and `PUT /clients/{id}` ignores `points`, so the ledger cannot be bypassed. To bring in an existing balance, use the adjustment.
- [x] 1.5 [BACK] `GET /clients/{id}/statement?page=&size=` (newest first, max 100 per page), scoped like the client itself.

## 2. Reward configuration
- [x] 2.1 [BACK] `V8`: `reward_mode` (default CATALOG), `points_to_currency_rate` (default 0.10), `max_discount_percent` (default 50), with CHECK constraints.
- [x] 2.2 [BACK] `reward` table and `/rewards` CRUD (`DELETE` deactivates). Owner/admin write, any user of the establishment reads; stock null means unlimited.
- [x] 2.3 [BACK] `GET/PUT /establishments/{id}/reward-settings` (owner/admin write; rate > 0, percent 1..100; omitted values keep their current setting).

## 3. Redemption
- [x] 3.1 [BACK] `POST /redemptions` (CATALOG only) locks client and reward, checks active/stock/balance, writes REDEEM, and returns an 8-character voucher; `POST /redemptions/{id}/use` marks it delivered once; `GET /redemptions` lists them.
- [x] 3.2 [BACK] `POST /purchases` accepts `redeemPoints`; discount = points x rate (rounded down to the cent), capped by `max_discount_percent`; points are earned on the amount actually paid. `GET /clients/{id}/redeemable?amount=` previews the maximum.
- [x] 3.3 [BACK] CASHBACK shares the DISCOUNT mechanism (same rate, cap and rounding: everything rounds DOWN, to the cent). Points already are the client's money balance (`balanceValue` in the preview); the only differences are how the frontend presents it and that a 100% cap is typical. No separate wallet was added: it would be a second balance to keep consistent with the ledger.
- [x] 3.4 [BACK] DISCOUNT/CASHBACK reject catalog redemptions and CATALOG rejects `redeemPoints`, both with 409.

## 4. Frontend
- [x] 4.1 [FRONT] Purchase screen asks the backend how many points may be used, offers "Usar máximo" and shows the discount and the amount to pay.
- [x] 4.2 [FRONT] `Catálogo de Prêmios` (owner/admin) and `Resgatar Brinde` (everyone; shows the voucher code and "marcar como entregue").
- [x] 4.3 [FRONT] Statement on the client detail page; `Configurar Recompensas` (owner/admin); balance adjustment with a mandatory reason on the client edit page.
- [x] 4.4 [FRONT] Date, discount, points and cancelled status on the purchase list/details, plus a cancel button for owners/admins. The dashboard ignores cancelled purchases and counts revenue net of the points discount. Verified with `tsc`, `vite build` and eslint in a Node container (lint problems 32 -> 28, none new); not exercised in a browser.

## 5. Tests
- [x] 5.1 [BACK] Ledger sum equals the balance in every flow (`RewardsLedgerIntegrationTest.assertLedgerMatchesBalance`); 8 concurrent redemptions with stock 3 give exactly 3 successes, and 6 racing for a balance of 100 give exactly 2. Also confirmed on PostgreSQL.
- [x] 5.2 [BACK] Unit tests per mode and for rounding (`PurchaseServiceTest`, `RewardCalculatorTest`), HTTP tests for catalog and discount/cashback flows.

## Deviations and follow-ups
- A reward's stock cannot be set back to "unlimited" once a number was set (an omitted field means "unchanged").
- Owner-only actions (adjustments, cancellations, catalog and settings writes) are enforced on the server; staff can register purchases and redeem/hand over rewards.
- No screen lists the redemptions of a whole establishment (only per client); `GET /redemptions` already provides them.
