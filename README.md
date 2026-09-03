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

## 🗄️ Banco de Dados

Esta API utiliza o banco de dados em memória **H2** para fins de desenvolvimento e teste. Os dados são inicializados a partir dos arquivos de migration em `src/main/resources/db/migration` sempre que a aplicação é iniciada.

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

Como o H2 é em memória, o seed é recriado a cada startup.

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
- 💾 H2 Database
- ✈️ Flyway
- 🛠️ Gradle
- ✨ Lombok
