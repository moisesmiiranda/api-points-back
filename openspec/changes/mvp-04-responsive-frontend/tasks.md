## 1. Layout
- [x] 1.1 [FRONT] `AppShell` replaces the fixed `marginRight: 280px` hack: permanent sidebar above 1024px; below it a top bar with a hamburger button and a drawer (backdrop, Esc, closes on navigation, hidden from the Tab order while closed, body scroll locked). Skip-to-content link added.
- [x] 1.2 [FRONT] Breakpoints at 1024/768/640 px. Touch targets >= 44px (>= 40px for small buttons), 16px inputs (no iOS zoom), forms and detail grids in one column, stacked buttons on phones, toasts full width. Measured: no control under 40px on any screen.
- [x] 1.3 [FRONT] `ResponsiveTable`: horizontal scroll on tablets and cards on phones, with each cell labelled from its column header (`data-label`), so no table had to repeat its headings. Pagination buttons are 44px on touch screens.
- [x] 1.4 [FRONT] Every form control has `<label htmlFor>`; visible focus rings; sidebar group titles are real buttons (`aria-expanded`); `ConfirmProvider`/`useConfirm` (accessible `alertdialog`, focus trap, Esc, focus restored) replaces all three `window.confirm`; toasts are `aria-live`; contrast fixed to WCAG AA (muted text, light-theme danger/warning/secondary, and darker fills for white-text buttons); `prefers-reduced-motion` respected.
- [x] 1.5 [FRONT] Ran a real Chromium + WebKit matrix at 360/768/1280 px with Playwright (84 checks, script in `scripts/responsive-check.mjs`, results in `docs/testes-responsivos.md`): no horizontal overflow, no small touch targets, no clipped elements, drawer/dialog/expired-session flows OK, no console errors. Not covered: a physical iOS device, screen readers, and the light theme visually.

## 2. Robustness
- [x] 2.1 [FRONT] 401 on an authenticated call clears the token and sends the user to `/login` with a "Sua sessão expirou" notice (a 401 on the login call itself is a wrong password and is left alone); 403, 5xx and network failures raise a toast. The same fixed messages are used by the pages, and the toast layer drops duplicates, so one failure shows once.
- [x] 2.2 [FRONT] The Dashboard now shows an error state with a retry button; the other listed pages already toasted their errors (only the Dashboard catch was really empty).
- [x] 2.3 [FRONT] `ErrorBoundary` around the routes (resets on navigation, offers reload/try again).
- [x] 2.4 [FRONT] No `any` left, no `escape()` (UTF-8 JWT decoding via `TextDecoder`), `location.state` is typed. ESLint went from 26 errors to 0 (6 old exhaustive-deps warnings remain). Also fixed two React errors found on the way: state updates during render/effects in `AuthContext`, `useEstablishmentsForRole` and `PrivateRoute`.

## 3. Production readiness
- [x] 3.1 [FRONT] `VITE_API_URL` (build time) is the axios `baseURL`, falling back to `/api`; the Vite proxy is kept for development (default target now 8081). Documented in `.env.example` and the README.
- [x] 3.2 [FRONT] `lang="pt-BR"`, title "Points Manager", meta description, theme color; `package.json` renamed to `points-back-front` 0.1.0; README updated (port 8081, PostgreSQL, dev-only accounts, production, tests).
- [x] 3.3 [FRONT] Web manifest, generated icons (192/512/maskable/apple-touch) and `public/sw.js`: caches only same-origin static files (`/assets`, `/icons`) and the app shell; never `/api`. Registered in production only. Not verified as an installed app on a device.
- [x] 3.4 [FRONT] Vitest + Testing Library (`npm test`): 65 tests over login, route guards, the 401/expired-session flow, API error events, the app shell/drawer, confirm dialog, error boundary, toasts, responsive tables and the CPF/JWT/error utilities.

## Follow-ups
- Tablets (768px) still scroll wide tables horizontally instead of using cards; acceptable, but a candidate for cards if users complain.
- Load-time: the bundle is a single ~700 kB chunk (Recharts); route-level code splitting would help slow phones.
- The stale `PrivateRoute`-expiry path relies on the Login page clearing the old token (a component cannot change state while rendering).
