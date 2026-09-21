## 1. Application readiness
- [ ] 1.1 [BACK] CORS: enable `http.cors(...)` in `SecurityFilterChain`, origins from env, allow `Authorization` and preflight without auth.
- [ ] 1.2 [BACK] Add Actuator: `/actuator/health` (liveness/readiness) public, everything else locked down.
- [ ] 1.3 [BACK] JSON logs, log levels per profile, no sensitive data (CPF, tokens) in logs.
- [ ] 1.4 [BACK] JVM memory limits (`-Xmx`, container memory) suited to a ~1GB VM; document the sizing.
- [ ] 1.5 [BACK/FRONT] Integrate error monitoring (Sentry free tier or equivalent) in both apps.

## 2. Packaging
- [ ] 2.1 [FRONT] Build static bundle with `VITE_API_URL`; either deploy to a free static host or serve via Caddy.
- [ ] 2.2 [INFRA] `docker-compose.prod.yml`: api, postgres (named volume, not exposed), caddy (HTTPS, reverse proxy, security headers).
- [ ] 2.3 [INFRA] Env/secret management (`.env` on the server, never committed).

## 3. Hosting
- [ ] 3.1 [INFRA] Validation stage: verify current AWS free-tier/credit terms and expiry date; provision a small VM (or another free host); firewall (80/443/SSH only).
- [ ] 3.2 [INFRA] Sales stage: choose a cheap reliable paid host + domain; write the migration steps (same compose, new VM, DNS switch).
- [ ] 3.3 [INFRA] Domain and DNS records; HTTPS validated.

## 4. Backup and recovery
- [ ] 4.1 [INFRA] Daily `pg_dump` to object storage with retention (e.g. 7 daily + 4 weekly); alert on failure.
- [ ] 4.2 [INFRA] Perform and document a restore drill on a clean machine.

## 5. CI/CD
- [ ] 5.1 [INFRA] Backend workflow: `./gradlew build` (tests + coverage gate), no `-x test` on the release image.
- [ ] 5.2 [INFRA] Frontend workflow: lint + build (+ tests when available).
- [ ] 5.3 [INFRA] Deploy step (image push + `docker compose pull && up -d`), manual approval for prod.

## 6. Operations
- [ ] 6.1 [INFRA] Uptime monitor on `/actuator/health` with email/WhatsApp alert.
- [ ] 6.2 [INFRA] Short runbook: deploy, rollback, restore, rotate secrets, where logs are.
- [ ] 6.3 [INFRA] Smoke test checklist after each deploy (login, purchase, redeem).
