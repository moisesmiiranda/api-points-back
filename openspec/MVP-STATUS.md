# Status do MVP: para retomar depois

Atualizado em **2026-09-20**. Roteiro e ordem: `MVP-ROADMAP.md`. Tarefas de cada change: `changes/mvp-0N-*/tasks.md`
(cada uma tem notas do que foi feito e dos desvios do plano).

## Onde estamos

| Change | Estado |
|---|---|
| `mvp-01-secure-foundation` | **Feito** |
| `mvp-02-client-identity` | **Feito** |
| `mvp-03-points-ledger-rewards` | **Feito** |
| `mvp-04-responsive-frontend` | **Feito** |
| `mvp-05-account-lifecycle` | **Feito** |
| `mvp-06-production-deploy` | Falta (19 tarefas) |
| `mvp-07-lgpd-legal` | Falta (11 tarefas) |

Depois do que foi feito o produto cobre: login com papéis, isolamento por estabelecimento, cliente por CPF (uma pessoa em
vários estabelecimentos, com grupos opcionais), extrato de pontos, recompensas em 3 modos (catálogo, desconto, cashback),
painel responsivo (celular, tablet, desktop), recuperação de senha e convite por e-mail, troca obrigatória de senha
provisória, e situação comercial do estabelecimento (teste, ativo, suspenso).

## Estado do código

- **Backend** (`api-points-back`): branch `develop`, árvore limpa. O PR #3 (`feature/mvp-01-secure-foundation`) já foi
  mesclado e as migrations V7 a V9 já estão no repositório. Migrations: V1, V3, V4, V6 a V9 (schema) e `db/dev/` V2, V5 (demo, só perfil `dev`).
  333 testes passando, gate de 90% de cobertura por `*Service` ok.
- **Frontend** (`points-back-front`): branch `main` com **~75 arquivos ainda não commitados** (tudo dos changes 02 a 05). 101 testes
  (Vitest), `tsc` limpo, lint com 0 erros e 6 avisos antigos.
- Não existe CI. O front e o back foram verificados em PostgreSQL real, com SMTP real (Mailpit) e em Chromium e WebKit.

## Como rodar para testar (desenvolvimento)

```bash
# 1. banco
docker compose up -d db

# 2. backend (perfil dev: usuários de demonstração, e-mails vão para o LOG)
cd api-points-back
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun          # http://localhost:8081

# 3. frontend
cd points-back-front
cp .env.example .env    # VITE_API_TARGET=http://localhost:8081
npm install && npm run dev                            # http://localhost:5173
```

- Contas: `admin@pointsback.local` / `ChangeMe123!` (admin), e `owner.super@pointsback.local`, `bruno.super@pointsback.local`
  (funcionário) e outras, todas com `Test1234!` (lista no README do front).
- **E-mails em dev**: aparecem no log do backend (`[email:dev] ... link`). Para ver e-mails de verdade use o Mailpit:
  `docker run -p 1025:1025 -p 8025:8025 axllent/mailpit` e suba o backend **sem** o perfil dev, com `MAIL_HOST=localhost MAIL_PORT=1025
  MAIL_SMTP_AUTH=false MAIL_FROM=no-reply@x.com APP_BASE_URL=http://localhost:5173 JWT_SECRET=... ADMIN_EMAIL=... ADMIN_PASSWORD=... DB_PASSWORD=...`
  (sem o perfil dev não há usuários de demonstração: entre com o admin do `ADMIN_EMAIL`).
- Testes: `./gradlew build` (backend) e `npm test` (frontend, precisa de Node; sem Node local use o contêiner `node:22-alpine`).
- Roteiros de navegador: `scripts/responsive-check.mjs` e `scripts/account-lifecycle-check.mjs` no front (ver `docs/testes-responsivos.md`).

## O que falta e o que pode incomodar no teste

**Próximos passos:** `mvp-06` (deploy, HTTPS, CORS, backup, health check, CI, monitoramento) e `mvp-07` (LGPD: termos, exclusão e
exportação de dados, auditoria). O `mvp-07` pode andar em paralelo (a parte jurídica não depende de código).

**Decisões suas antes do `mvp-06`:** hospedagem paga, domínio, provedor de e-mail (conferir os limites gratuitos atuais, não verificados),
e se o front vai em domínio próprio da API ou no mesmo domínio.

**Limitações conhecidas (anotadas nos `tasks.md`):**
- Tablets (768 px) rolam tabelas largas na horizontal em vez de virar cartões; bundle do front de ~730 kB (sem code splitting).
- Sem tela de resgates do estabelecimento inteiro (só por cliente); estoque de prêmio não volta a "ilimitado".
- Sem tela de admin para grupos de estabelecimentos e compartilhamento de clientes (só pela API).
- Sem botão "reenviar convite" (use "Esqueci minha senha").
- Limitadores de tentativa são em memória (valem por instância; atrás de proxy o IP visto é o do proxy até configurar os cabeçalhos).
- CORS do backend ainda só libera `localhost:8081` (o `mvp-06` trata); o `HEALTHCHECK` do Docker depende do Actuator (`mvp-06`).
- Falha de envio de e-mail só é logada (por desenho): alertar no `mvp-06`.
- Não testado em aparelho iOS físico, com leitor de tela, nem visualmente no tema claro.
- Nada de cobrança automática, autocadastro, landing page, portal do consumidor (fora do MVP, ver `MVP-ROADMAP.md`).
