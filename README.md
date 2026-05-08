# PointsBack API 🚀

API para gerenciamento de clientes e estabelecimentos.

## 📥 Clonando o repositório

## Branch DEVELOP
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

## 📚 Endpoints disponíveis

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
