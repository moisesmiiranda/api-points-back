## Why

Users will operate on PC, tablet and phone, but the panel has zero media queries: the fixed sidebar and layout are unusable on small screens. Errors are also swallowed and expired sessions are not handled.

## What Changes

- Mobile-first responsive layout: drawer sidebar, touch-sized controls, responsive tables/cards and forms.
- Global 401/403/5xx handling, error boundary, no silent `catch {}`.
- Production `baseURL` via env var; cleanup of title/lang/package name.
- (SHOULD) PWA manifest + service worker for home-screen install.

## Capabilities

### New Capabilities
- `responsive-panel`
### Modified Capabilities
- None.

## Impact

Frontend only (`points-back-front`): `index.css`, `App.tsx`, `Sidebar`, list/form pages, `services/api.ts`, `AuthContext`, `index.html`, `vite.config.ts`.
Can run in parallel with `mvp-01`; must land before `mvp-06` go-live.
