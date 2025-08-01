# PointsBack API 🚀

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

Para rodar a aplicação:

```bash
./gradlew bootRun
```

## 📚 Endpoints disponíveis

### 👤 Clientes

- `POST /clients`  
  Cria um novo cliente.  
  Corpo esperado:  
  ```json
  {
    "name": "Nome",
    "email": "email@exemplo.com",
    "phone": "11999999999",
    "cpf": "12345678900"
  }
  ```

- `GET /clients/all`  
  Lista todos os clientes.

- `GET /clients/{id}`  
  Busca um cliente pelo ID.

### 🏢 Estabelecimentos

- `POST /establishments`  
  Cria um novo estabelecimento.  
  Corpo esperado:  
  ```json
  {
    "name": "Nome",
    "email": "email@exemplo.com",
    "phone": "11999999999",
    "cnpj": "12345678000199"
  }
  ```

- `GET /establishments/list`  
  Lista todos os estabelecimentos.

- `GET /establishments/{id}`  
  Busca um estabelecimento pelo ID.

## 🧰 Tecnologias utilizadas

- ☕ Java 17+
- 🌱 Spring Boot
- 🗄️ Spring Data JPA
- 🛠️ Gradle
- ✨ Lombok
