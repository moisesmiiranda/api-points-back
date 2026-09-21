# PointsBack API DEVELOP🚀 

API para gerenciamento de clientes e estabelecimentos.

## 📥 Clonando o repositório

```bash
git clone https://github.com/moisesmiiranda/api-points-back.git
cd api-points-back
```

## 🛠️ Buildando a aplicação

Certifique-se de ter o Java 21+ instalado (o Gradle vem pelo `./gradlew`).

```bash
./gradlew build
```
Para rodar os testes:
```bash 
./gradlew test 
```

Para rodar a aplicação em desenvolvimento (perfil `dev`, com dados de demonstração e segredos de dev):

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

> **Sem o perfil `dev`, a aplicação sobe em modo "produção"**: nenhum segredo tem valor padrão e ela
> recusa iniciar se `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` ou `ADMIN_PASSWORD` estiverem ausentes
> (veja `.env.example`). Os dados de demonstração e as contas de teste só existem no perfil `dev`.

## 🐳 Rodando via Docker Compose

Sobe a API e um banco **PostgreSQL** juntos, com as migrations do Flyway aplicadas automaticamente:

```bash
docker compose up --build
```

A API fica disponível em `http://localhost:8081` e o Postgres em `localhost:5432`
(banco/usuário/senha padrão: `pointsback`/`pointsback`/`pointsback`).

O `docker-compose.yml` deste repositório é a stack **local**: ele ativa o perfil `dev`. O Postgres é
publicado apenas em `127.0.0.1`. Para um deploy real, não use o perfil `dev` e forneça os segredos.

Variáveis de ambiente aceitas pelo `docker-compose.yml` (todas com valor padrão de dev):
`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

Os dados ficam persistidos no volume `pointsback-db-data` entre reinicializações. Para
recomeçar do zero (reaplicar as migrations em um banco vazio):

```bash
docker compose down -v
docker compose up --build
```

## 📊 Qualidade de código (SonarQube)

O `docker-compose.yml` também sobe um **SonarQube Community Edition** (com banco H2 embutido —
adequado para uso local, não para produção) para medir cobertura de testes, bugs, code smells,
vulnerabilidades e duplicação.

```bash
docker compose up -d sonarqube
```

Acesse `http://localhost:9000`.

- **Usuário padrão**: `admin`
- **Senha padrão**: `admin`

No primeiro login o SonarQube obriga a troca dessa senha. Depois, gere um token de análise em
**My Account → Security** (ou `http://localhost:9000/account/security`) e use-o para rodar o
scanner:

```bash
SONAR_TOKEN=<seu-token> ./gradlew sonar
```

Isso reaproveita o relatório do JaCoCo (`build/reports/jacoco/test/jacocoTestReport.xml`, gerado
junto com `./gradlew test`) e envia as métricas para o dashboard do projeto em
`http://localhost:9000/dashboard?id=api-points-back`.

> Se estiver rodando o scanner de dentro de um container (em vez da máquina host), aponte para o
> serviço pelo nome na rede do compose: `./gradlew sonar -Dsonar.host.url=http://sonarqube:9000`.

## 🗄️ Banco de Dados

- **Via `docker compose`** ou **`./gradlew bootRun`**: usam **PostgreSQL**. Rodando fora do
  Docker, é preciso ter um Postgres acessível (ex: `docker compose up -d db`) e, se os valores
  não forem os padrões de dev, definir `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`.
  Os dados são inicializados a partir das migrations em `src/main/resources/db/migration` no
  primeiro startup. As migrations de dados de demonstração ficam em `src/main/resources/db/dev`
  e só são aplicadas com o perfil `dev`.
- **Testes (`./gradlew test`)**: usam o banco em memória **H2**, configurado em
  `src/test/resources/application.yml` — sem necessidade de nenhum serviço externo.

## 🔐 Autenticação e Autorização

Todos os endpoints, exceto `POST /auth/login`, exigem um JWT válido no header `Authorization: Bearer <token>`.

Existem três papéis (`role`): `PLATFORM_ADMIN`, `ESTABLISHMENT_OWNER` e `ESTABLISHMENT_STAFF`. `ESTABLISHMENT_OWNER` e `ESTABLISHMENT_STAFF` só enxergam/gerenciam dados do próprio estabelecimento; `PLATFORM_ADMIN` tem acesso irrestrito. Veja `openspec/changes/add-jwt-authentication/specs/authorization/spec.md` para as regras completas.

### Login

```
POST /auth/login
{
  "email": "admin@pointsback.local",
  "password": "ChangeMe123!"
}
```

Retorna `{ "accessToken": "...", "tokenType": "Bearer", "expiresInMinutes": 60 }`.

### Contas de teste (seed) — somente perfil `dev`

A migration `db/dev/V5__seed_users.sql` (carregada apenas com `SPRING_PROFILES_ACTIVE=dev`) popula a tabela `users` com contas de demonstração para exercitar
a gestão de usuários. **Todas usam a senha `Test1234!`** (o `PLATFORM_ADMIN` do bootstrap continua
sendo `admin@pointsback.local` / `ChangeMe123!` e não é inserido pela migration).

| Email | Papel | Estabelecimento | Ativo |
|---|---|---|---|
| `admin2@pointsback.local` | `PLATFORM_ADMIN` | — | sim |
| `owner.super@pointsback.local` | `ESTABLISHMENT_OWNER` | 1 (Supermarket A) | sim |
| `bruno.super@pointsback.local` / `carla.super@pointsback.local` | `ESTABLISHMENT_STAFF` | 1 | sim |
| `diego.super@pointsback.local` | `ESTABLISHMENT_STAFF` | 1 | não |
| `owner.resto@pointsback.local` | `ESTABLISHMENT_OWNER` | 2 (Restaurant B) | sim |
| `elena.resto@pointsback.local` / `felipe.resto@pointsback.local` | `ESTABLISHMENT_STAFF` | 2 | sim |
| `gabi.resto@pointsback.local` | `ESTABLISHMENT_STAFF` | 2 | não |

Como as migrations do Flyway rodam apenas uma vez por banco (controle via
`flyway_schema_history`), o seed é inserido no primeiro startup contra um banco vazio.
Com `docker compose`, os dados persistem no volume entre reinicializações; para recriá-los,
rode `docker compose down -v` antes de subir novamente.

### Variáveis de ambiente

Defina estas variáveis em qualquer ambiente que não seja local/dev. Fora do perfil `dev` elas são
**obrigatórias** (não há padrão) e a aplicação valida no startup: `JWT_SECRET` com no mínimo 32
caracteres e diferente do valor de dev, `ADMIN_PASSWORD` com no mínimo 12 caracteres e diferente do padrão.

| Variável | Descrição | Padrão (só no perfil dev) |
|---|---|---|
| `DB_PASSWORD` | Senha do Postgres | `pointsback` |
| `JWT_SECRET` | Chave usada para assinar os JWTs (mín. 32 bytes) | valor de desenvolvimento embutido |
| `JWT_EXPIRATION_MINUTES` | Tempo de expiração do token, em minutos | `60` |
| `ADMIN_EMAIL` | E-mail da conta `PLATFORM_ADMIN` inicial, criada automaticamente no primeiro startup se ainda não existir | `admin@pointsback.local` |
| `ADMIN_PASSWORD` | Senha da conta `PLATFORM_ADMIN` inicial | `ChangeMe123!` |
| `APP_BASE_URL` | URL pública do frontend: os links dos e-mails apontam para ela (**obrigatória** fora do perfil `dev`) | `http://localhost:5173` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH` | Envio de e-mail por SMTP (qualquer provedor). `MAIL_HOST` e `MAIL_FROM` são **obrigatórios** fora do perfil `dev` | `dev`: os e-mails vão para o log |
| `PASSWORD_RESET_TTL_MINUTES` / `INVITE_TTL_HOURS` / `PASSWORD_RESET_MAX_REQUESTS` | Validade do link de recuperação (60 min), do convite (72 h) e pedidos de recuperação por 15 min (5) | `60` / `72` / `5` |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_WINDOW_MINUTES` | Limite de tentativas de login falhas por IP+e-mail e a janela em minutos | `5` / `15` |

## 📚 Endpoints disponíveis

### 🔑 Senha, convites e situação do estabelecimento

Roteiro de implantação de um cliente novo: [docs/onboarding-concierge.md](docs/onboarding-concierge.md).

- `POST /auth/forgot-password` — `{ "email" }`. **Sempre responde 200** com a mesma mensagem, exista ou não o
  e-mail (e não envia nada se não existir ou se a conta estiver desativada). Limite de 5 pedidos por 15 minutos por
  e-mail e origem (`429`). Envia um link (`APP_BASE_URL/redefinir-senha?token=...`) válido por 60 minutos.
- `POST /auth/reset-password` — `{ "token", "newPassword" }` (8 a 72 caracteres). O link é de **uso único**; um link
  novo cancela os anteriores. Link inválido, vencido ou usado responde `400` com a mesma mensagem. A troca encerra todas
  as sessões abertas da pessoa. Só o hash SHA-256 do token fica no banco.
- `POST /users/me/password` — `{ "currentPassword", "newPassword" }`. Devolve um token novo (as demais sessões
  terminam). Senha atual errada conta no limite de tentativas de login.
- **Senha provisória**: um usuário criado **com** `password` (ou que teve a senha trocada por um gerente) entra com
  `mustChangePassword: true` e só consegue `GET /users/me` e `POST /users/me/password` (`403` no resto) até trocá-la.
  Criado **sem** `password`, recebe um convite por e-mail (72 h) para escolher a própria senha.
- `GET /establishments/{id}/plan` — situação comercial: `status` (`TRIAL`, `ACTIVE`, `SUSPENDED`), `effectiveStatus`
  (um teste vencido vale como `SUSPENDED`), `plan`, `trialEndsAt`, `trialDaysLeft`. O dono vê só o próprio.
- `PUT /establishments/{id}/plan` — só `PLATFORM_ADMIN`: `{ "status", "plan", "trialEndsAt" }`. Sem `trialEndsAt`, um
  teste novo dura 14 dias. Estabelecimentos existentes foram migrados como `ACTIVE`; os novos nascem em `TRIAL`.
- **Suspenso**: o login responde `402 Payment Required` (depois de conferir a senha) e as sessões já abertas recebem
  `402` em tudo, exceto `GET /users/me` e o `GET` do próprio plano. Vale na próxima chamada, sem esperar o token expirar.

### 👥 Usuários

Acesso: `PLATFORM_ADMIN` gerencia qualquer conta. `ESTABLISHMENT_OWNER` só enxerga e gerencia os
`ESTABLISHMENT_STAFF` do próprio estabelecimento e não pode alterar `role`/`establishmentId`.
`ESTABLISHMENT_STAFF` não acessa nenhum desses endpoints (exceto `/users/me`).

- `GET /users/me` — perfil do usuário autenticado (qualquer papel).
- `GET /users` — `PLATFORM_ADMIN` recebe todas as contas; `ESTABLISHMENT_OWNER` recebe apenas os
  `ESTABLISHMENT_STAFF` do seu estabelecimento.
- `POST /users` — cria uma conta. `PLATFORM_ADMIN` (qualquer papel) ou `ESTABLISHMENT_OWNER`
  (apenas `ESTABLISHMENT_STAFF` do próprio estabelecimento).
  ```json
  { "name": "Jane", "email": "jane@x.com", "password": "Secret1234", "role": "ESTABLISHMENT_STAFF", "establishmentId": 1 }
  ```
  `password` é opcional: sem ele a pessoa recebe um convite por e-mail. Com ele, a senha é provisória e precisa ser
  trocada no primeiro acesso.
  ```json
  { "name": "Jane", "email": "jane@x.com", "role": "ESTABLISHMENT_STAFF", "establishmentId": 1 }
  ```
- `PUT /users/{id}` — atualização parcial (só os campos não nulos). Corpo aceita `name`, `email`,
  `password`, `role`, `establishmentId`, `active`. Só `PLATFORM_ADMIN` pode mudar `role`/`establishmentId`
  (e trocar para um papel diferente de `PLATFORM_ADMIN` exige `establishmentId`).
- `DELETE /users/{id}` — desativação soft (`active = false`), retorna `204`.

### 👤 Clientes

- `POST /clients`  
  Cria um novo cliente.  
  Corpo esperado:  
  ```json
  {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "889988889988",
    "cpf": "123.456.789-00"
  }
  ```

- `GET /clients/all`  
  Lista todos os clientes. 
  Exemplo de resposta:
  ```json
  [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john.doe@example.com",
      "phone": "889988889988",
      "cpf": "123.456.789-00",
      "points": 15
    },
    {
      "id": 2,
      "name": "Jane Smith",
      "email": "jane.smith@example.com",
      "phone": "98765432100",
      "cpf": "987.654.321-00",
      "points": 1
    }
  ]
  ```
- `GET /clients/{id}`  
  Busca um cliente pelo ID.

- `GET /clients/search?cpf=&phone=`  
  Busca clientes por CPF e/ou telefone (com ou sem pontuação; ao menos um filtro é obrigatório).
  Só enxerga o próprio estabelecimento; `PLATFORM_ADMIN` busca em todos (ou filtra com `establishmentId`).

- `POST /clients/import` — corpo `{ "cpf": "529.982.247-25" }`  
  Cria a conta do cliente no estabelecimento do chamador a partir de um cliente de outro
  estabelecimento do **mesmo grupo**. Exige que os dois lados tenham ativado o compartilhamento;
  copia só os dados de contato e o saldo começa em zero. `404` se ninguém do grupo compartilha esse CPF.

#### Identidade do cliente (CPF)

O CPF identifica a **pessoa** (`person`, guardado só com dígitos); a mesma pessoa pode ser cliente de
vários estabelecimentos. Cada estabelecimento tem a sua própria conta (`client`) com os dados de
contato que ele cadastrou e o seu saldo de pontos, e nunca vê os de outro. Cadastrar o mesmo CPF duas
vezes no mesmo estabelecimento retorna `409`. O CPF é devolvido formatado (`529.982.247-25`).

### 🔗 Grupos de estabelecimentos

Estabelecimentos de um mesmo grupo (ex.: filiais) podem, se quiserem, compartilhar clientes.

- `POST /establishment-groups` / `GET /establishment-groups` — só `PLATFORM_ADMIN`.
- `PUT /establishments/{id}/group` — `PLATFORM_ADMIN`; corpo `{ "groupId": 1 }` (ou `null` para sair do
  grupo, o que também desliga o compartilhamento).
- `GET /establishments/{id}/sharing` — grupo atual e se compartilha clientes.
- `PUT /establishments/{id}/sharing` — corpo `{ "shareClients": true }`; só o dono (ou admin), e só
  para estabelecimentos que já estão em um grupo.

### 🏢 Estabelecimentos

- `POST /establishments`  
  Cria um novo estabelecimento.  
  Corpo esperado:  
  ```json
  {
    "name": "Supermarket A",
    "email": "supermarketA@mail.com",
    "phone": "123-456-7890",
    "cnpj": "11.111.111/0001-11",
    "valuePerPoint": 10
  }
  ```
- `GET /establishments/list`  
  Lista todos os estabelecimentos.

- `GET /establishments/{id}`  
  Busca um estabelecimento pelo ID.
  
### 🛒 Compras

Cada compra guarda a data (`createdAt`), o total da venda (`amount`), o que foi pago com pontos e os pontos
ganhos. Os pontos são **1 por cada `valuePerPoint` inteiro pago** (arredondado para baixo), calculados sobre o
valor efetivamente pago, ou seja, `amount` menos o desconto.

- `POST /purchases` — registra uma compra e devolve o que foi gravado.
  ```json
  { "clientId": 1, "amount": 100.00, "redeemPoints": 300 }
  ```
  `establishmentId` é opcional (o do usuário) e obrigatório para `PLATFORM_ADMIN`. `redeemPoints` (opcional)
  usa pontos para pagar parte da compra; só funciona nos modos `DISCOUNT` e `CASHBACK` e nunca acima do
  máximo configurado (veja a seção de recompensas). Resposta de exemplo:
  ```json
  { "purchaseId": 7, "clientId": 1, "establishmentId": 1, "amount": 100.00, "redeemPoints": 300,
    "discountAmount": 30.00, "pointsEarned": 7, "createdAt": "2026-09-20T20:12:12", "cancelledAt": null }
  ```
- `GET /purchases` — lista as compras do estabelecimento (todas, para `PLATFORM_ADMIN`), inclusive canceladas.
- `GET /purchases/{id}` — detalhe.
- `PUT /purchases/{id}` — corrige `clientId`, `establishmentId` e/ou `amount`. Os pontos são recalculados e
  lançados no extrato. Não é possível editar uma compra cancelada nem uma paga com pontos (`409`).
- `POST /purchases/{id}/cancel` — só dono/admin. Devolve os pontos usados e estorna os ganhos. Se o cliente
  já gastou os pontos que a compra deu, retorna `422` e nada muda.

### ⭐ Pontos e extrato

O saldo (`points`) só muda por compra, resgate, cancelamento ou ajuste manual, e **toda mudança gera uma
linha no extrato** (`points_ledger`, somente inserção): o saldo é sempre a soma do extrato. O saldo nunca fica
negativo (`422`). Criar ou editar um cliente **não** altera pontos.

- `POST /clients/{id}/points/adjust` — só dono/admin; corpo `{ "points": -20, "reason": "Pontos vencidos" }`
  (valor com sinal, diferente de zero; o motivo é obrigatório). Substitui o antigo `PUT /clients/{id}/points`.
- `GET /clients/{id}/statement?page=0&size=20` — extrato, do mais novo para o mais antigo (máx. 100 por página).
  Tipos: `CREDIT` (compra), `REDEEM` (resgate), `ADJUST` (ajuste manual), `REVERSAL` (estorno/correção).
- `GET /clients/{id}/redeemable?amount=100.00` — quantos pontos (e quanto em reais) o cliente pode usar numa
  compra desse valor.

### 🎁 Recompensas

Cada estabelecimento escolhe **um modo** para os pontos:

| Modo | Como funciona |
|---|---|
| `CATALOG` (padrão) | O cliente troca pontos por brindes/cupons do catálogo e recebe um voucher. |
| `DISCOUNT` | Os pontos abatem o valor da compra (`redeemPoints`). |
| `CASHBACK` | O mesmo mecanismo, apresentado como saldo em reais para compras futuras; normalmente com máximo de 100%. |

Em `DISCOUNT`/`CASHBACK`, `desconto = pontos × pointsToCurrencyRate` (arredondado para baixo, em centavos) e
não pode passar de `maxDiscountPercent` do valor da compra. O modo desligado responde `409`.

- `GET /establishments/{id}/reward-settings` / `PUT ...` (só dono/admin) — corpo
  `{ "rewardMode": "DISCOUNT", "pointsToCurrencyRate": 0.10, "maxDiscountPercent": 30 }` (taxa e percentual
  omitidos mantêm o valor atual).
- `POST /rewards`, `GET /rewards`, `GET /rewards/{id}`, `PUT /rewards/{id}`, `DELETE /rewards/{id}` — catálogo
  (`BRINDE` ou `CUPOM`, `pointsCost`, `stock` opcional). Só dono/admin altera; qualquer usuário do
  estabelecimento consulta. `DELETE` desativa (o histórico continua referenciando o prêmio).
- `POST /redemptions` — `{ "clientId": 1, "rewardId": 5 }` (só modo `CATALOG`). Debita os pontos, baixa o
  estoque e devolve um voucher de 8 caracteres (`code`). `409` se o prêmio está inativo ou sem estoque, `422`
  se faltam pontos.
- `GET /redemptions?clientId=` — resgates, do mais novo para o mais antigo.
- `POST /redemptions/{id}/use` — marca o voucher como entregue (uma única vez).

## 🧰 Tecnologias utilizadas

- ☕ Java 17+
- 🌱 Spring Boot
- 🗄️ Spring Data JPA
- 🐘 PostgreSQL (💾 H2 nos testes)
- ✈️ Flyway
- 🐳 Docker / Docker Compose
- 📊 SonarQube
- 🛠️ Gradle
- ✨ Lombok
