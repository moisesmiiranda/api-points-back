# PointsBack API DEVELOP🚀 

API para gerenciamento de clientes e estabelecimentos.

## 📥 Clonando o repositório

```bash
git clone https://github.com/moisesmiiranda/api-points-back.git
cd api-points-back
```

## 🛠️ Buildando a aplicação

Certifique-se de ter o Java 17+ e o Gradle instalados.

```bash
./gradlew build
```
Para rodar os testes:
```bash 
./gradlew test 
```

Para rodar a aplicação:

```bash
./gradlew bootRun
```

## 🐳 Rodando via Docker Compose

Sobe a API e um banco **PostgreSQL** juntos, com as migrations do Flyway aplicadas automaticamente:

```bash
docker compose up --build
```

A API fica disponível em `http://localhost:8081` e o Postgres em `localhost:5432`
(banco/usuário/senha padrão: `pointsback`/`pointsback`/`pointsback`).

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
  primeiro startup.
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

### Contas de teste (seed)

A migration `V5__seed_users.sql` popula a tabela `users` com contas de demonstração para exercitar
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

Defina estas variáveis em qualquer ambiente que não seja local/dev — os valores padrão em `application.yml` **não são seguros para produção**:

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `JWT_SECRET` | Chave usada para assinar os JWTs (mín. 32 bytes) | valor de desenvolvimento embutido |
| `JWT_EXPIRATION_MINUTES` | Tempo de expiração do token, em minutos | `60` |
| `ADMIN_EMAIL` | E-mail da conta `PLATFORM_ADMIN` inicial, criada automaticamente no primeiro startup se ainda não existir | `admin@pointsback.local` |
| `ADMIN_PASSWORD` | Senha da conta `PLATFORM_ADMIN` inicial | `ChangeMe123!` |

## 📚 Endpoints disponíveis

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
  { "name": "Jane", "email": "jane@x.com", "password": "secret", "role": "ESTABLISHMENT_STAFF", "establishmentId": 1 }
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
- `POST /purchases`  
  Registra uma nova compra.  
  Corpo esperado:  
  ```json
  {
    "clientId": 1,
    "establishmentId": 1,
    "purchaseValue": 150.75
  }
  ```
- `GET /purchases/all`
  Lista todas as compras.
  Exemplo de resposta:
  ```json
  [
    {
        "id": 1,
        "client": {
            "id": 1,
            "name": "John Doe",
            "email": "john.doe@example.com",
            "phone": "889988889988",
            "cpf": "123.456.789-00",
            "points": 15
        },
        "establishment": {
            "id": 1,
            "name": "Supermarket A",
            "email": "supermarketA@mail.com",
            "phone": "123-456-7890",
            "valuePerPoint": 10.0,
            "cnpj": "11.111.111/0001-11"
        },
        "amount": 150.75,
        "purchaseDate": "2023-10-27T10:00:00Z"
    },
    {
        "id": 2,
        "client": {
            "id": 2,
            "name": "Jane Smith",
            "email": "jane.smith@example.com",
            "phone": "98765432100",
            "cpf": "987.654.321-00",
            "points": 1
        },
        "establishment": {
            "id": 2,
            "name": "Restaurant B",
            "email": "restaurantB@mail.com",
            "phone": "321-345-8989",
            "valuePerPoint": 50.0,
            "cnpj": "22.222.222/0001-22"
        },
        "amount": 85.5,
        "purchaseDate": "2023-10-27T11:30:00Z"
    }
  ]
  ```


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
