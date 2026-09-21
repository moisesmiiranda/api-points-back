## Why

Today points can only accumulate and are changed by a free `PUT /clients/{id}/points`, purchases have no date, and there is no redemption. A loyalty product without redemption or history delivers no value. Establishments must choose how points pay off: cashback for future purchases, discount on purchases, or exchange for gifts/coupons.

## What Changes

- `purchase.created_at` and a `points_ledger` (append-only: credit, redeem, adjust, reversal; who, when, reason, reference); balance is derived/kept consistent with the ledger.
- Remove the free-form `PUT /clients/{id}/points`; replace with a controlled manual adjustment (OWNER only, with reason) that writes a ledger entry.
- `establishment.reward_mode` (`CASHBACK` | `DISCOUNT` | `CATALOG`) and its configuration; one mode active per establishment.
- Redemption engine: `CATALOG` (gifts/coupons), `DISCOUNT` (points -> discount at purchase), `CASHBACK` (points converted to a currency credit applied as discount on the next purchase, reusing the discount path).
- Client statement endpoint (history).

## Capabilities

### New Capabilities
- `points-ledger`, `rewards-redemption`
### Modified Capabilities
- `authorization`: redemption/adjustment role rules.

## Impact

`Purchase`, `Client(Account)`, `PurchaseService`, `Establishment`, new `RewardService`/controllers, Flyway migrations, frontend purchase flow and reward settings.
Depends on: `mvp-01-secure-foundation`, `mvp-02-client-identity`.
