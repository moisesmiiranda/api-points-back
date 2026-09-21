## Why

There is no way to run the product outside a developer machine: no frontend image, no CORS/proxy strategy, no HTTPS, backups, health checks, monitoring or CI. The goal is the cheapest stable setup for validation (free tier) that can move to a cheap paid host without rework.

## What Changes

- Portable Docker Compose stack: API + Postgres + Caddy (automatic HTTPS); frontend as static build on a free static host or served by Caddy.
- Correct CORS integrated in Spring Security; configurable allowed origins.
- Actuator health/readiness, structured logs, error monitoring (Sentry or similar).
- Daily automated Postgres backup to object storage, with a tested restore.
- CI pipeline for backend tests and frontend lint/build; deploy script.
- No provider-exclusive services, to avoid lock-in.

## Capabilities

### New Capabilities
- `deployment-operations`
### Modified Capabilities
- None.

## Impact

`docker-compose.prod.yml`, `Dockerfile`s, `WebConfig`/`SecurityConfig`, `application-prod.yml`, `.github/workflows`, docs/runbook, DNS/domain.
Depends on: `mvp-01`; go-live also needs `mvp-04`.
