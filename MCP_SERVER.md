# MCP Server for Points Back API

This is a Model Context Protocol (MCP) server implementation for the Points Back API. It exposes all API endpoints as MCP tools that can be called via JSON-RPC 2.0 protocol.

## Overview

The MCP server allows external clients (like Claude or other AI assistants) to interact with your Points Back API through a standardized protocol. It provides tool definitions and execution capabilities for:

- **Client Management**: Create, read, update clients and manage loyalty points
- **Establishment Management**: Create and manage establishments
- **Purchase Tracking**: Register purchases and track point accumulation

## API Endpoints

### MCP RPC Endpoint
- **URL**: `POST /mcp/rpc`
- **Content-Type**: `application/json`
- **Protocol**: JSON-RPC 2.0

### Health Check
- **URL**: `GET /mcp/health`
- **Returns**: Server status

## Available Tools

### Client Tools

#### `list_all_clients`
List all clients in the system.

**Request Example**:
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "list_all_clients",
    "arguments": {}
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": [
    {
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "11999999999",
      "cpf": "12345678901",
      "points": 100
    }
  ]
}
```

#### `get_client`
Get a specific client by ID.

**Parameters**:
- `id` (number, required): The client ID

**Request Example**:
```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/call",
  "params": {
    "name": "get_client",
    "arguments": {
      "id": 1
    }
  }
}
```

#### `create_client`
Create a new client.

**Parameters**:
- `name` (string, required): Client full name
- `email` (string, required): Client email address
- `phone` (string, required): Client phone number
- `cpf` (string, required): Client CPF (unique)
- `points` (number, optional): Initial points (defaults to 0)

**Request Example**:
```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/call",
  "params": {
    "name": "create_client",
    "arguments": {
      "name": "Jane Smith",
      "email": "jane@example.com",
      "phone": "11988888888",
      "cpf": "98765432101",
      "points": 50
    }
  }
}
```

#### `update_client`
Update client information.

**Parameters**:
- `id` (number, required): The client ID
- `name` (string, optional): Updated name
- `email` (string, optional): Updated email
- `phone` (string, optional): Updated phone
- `cpf` (string, optional): Updated CPF
- `points` (number, optional): Updated points

#### `add_points_to_client`
Add points to a client's account.

**Parameters**:
- `id` (number, required): The client ID
- `points` (number, required): Points to add

### Establishment Tools

#### `list_all_establishments`
List all establishments in the system.

#### `get_establishment`
Get a specific establishment by ID.

**Parameters**:
- `id` (number, required): The establishment ID

#### `create_establishment`
Create a new establishment.

**Parameters**:
- `name` (string, required): Establishment name
- `email` (string, required): Establishment email
- `phone` (string, required): Establishment phone
- `cnpj` (string, required): Establishment CNPJ (unique)
- `valuePerPoint` (number, optional): Value per point for calculations

#### `update_establishment`
Update establishment information.

**Parameters**:
- `id` (number, required): The establishment ID
- `name` (string, optional): Updated name
- `email` (string, optional): Updated email
- `phone` (string, optional): Updated phone
- `cnpj` (string, optional): Updated CNPJ
- `valuePerPoint` (number, optional): Updated value per point

### Purchase Tools

#### `list_all_purchases`
List all purchases in the system.

#### `get_purchase`
Get a specific purchase by ID.

**Parameters**:
- `id` (number, required): The purchase ID

#### `create_purchase`
Register a new purchase and award points to the client.

**Parameters**:
- `client_id` (number, required): The client ID
- `establishment_id` (number, required): The establishment ID
- `amount` (number, required): Purchase amount

**Note**: Points are automatically calculated based on the establishment's `valuePerPoint` setting.

#### `update_purchase`
Update purchase information.

**Parameters**:
- `id` (number, required): The purchase ID
- `client_id` (number, optional): Updated client ID
- `establishment_id` (number, optional): Updated establishment ID
- `amount` (number, optional): Updated amount

## Example Usage

### List All Tools
```bash
curl -X POST http://localhost:8081/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "list-tools",
    "method": "tools/list",
    "params": {}
  }'
```

### Create a Client
```bash
curl -X POST http://localhost:8081/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "create-client-1",
    "method": "tools/call",
    "params": {
      "name": "create_client",
      "arguments": {
        "name": "Maria Silva",
        "email": "maria@example.com",
        "phone": "11987654321",
        "cpf": "12345678901"
      }
    }
  }'
```

### Register a Purchase
```bash
curl -X POST http://localhost:8081/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "purchase-1",
    "method": "tools/call",
    "params": {
      "name": "create_purchase",
      "arguments": {
        "client_id": 1,
        "establishment_id": 1,
        "amount": 150.00
      }
    }
  }'
```

## Error Handling

The MCP server follows JSON-RPC 2.0 error handling standards:

### Error Response Example
```json
{
  "jsonrpc": "2.0",
  "id": "error-example",
  "error": {
    "code": -32602,
    "message": "Invalid params: Missing required parameter: id"
  }
}
```

### Error Codes
- `-32601`: Method not found
- `-32602`: Invalid params
- `-32603`: Internal error

## Architecture

### Components

1. **McpController** (`mcp/controller/McpController.java`)
   - Handles incoming JSON-RPC requests
   - Routes requests to appropriate handlers
   - Manages tool listing and execution

2. **McpToolRegistry** (`mcp/tool/McpToolRegistry.java`)
   - Registers all available tools
   - Provides tool metadata and input schemas
   - Validates tool availability

3. **McpToolExecutor** (`mcp/tool/McpToolExecutor.java`)
   - Executes tool calls
   - Handles parameter extraction and type conversion
   - Manages service calls

4. **Model Classes** (`mcp/model/`)
   - `McpRequest`: Request structure
   - `McpResponse`: Response structure
   - `McpError`: Error details
   - `McpTool`: Tool definition

## Running the Server

The MCP server runs as part of the main Spring Boot application:

```bash
./gradlew bootRun
```

The server will be available at `http://localhost:8081`

### Verify MCP Server is Running
```bash
curl http://localhost:8081/mcp/health
```

Expected response:
```json
{
  "status": "ok",
  "message": "MCP Server is running"
}
```

## Integration with Claude

To use this MCP server with Claude, configure it as an MCP server in your Claude client:

```json
{
  "mcpServers": {
    "points-back-api": {
      "command": "java",
      "args": ["-jar", "path/to/api-points-back.jar"],
      "env": {
        "PORT": "8081"
      }
    }
  }
}
```

Then use Claude to interact with your API through natural language:

**Example**: "Create a new client named Alice with email alice@example.com"

Claude will:
1. Understand the request
2. Call the appropriate MCP tool (`create_client`)
3. Extract parameters from your request
4. Execute the tool
5. Return the result in natural language

## Development

### Adding a New Tool

1. **Register the tool** in `McpToolRegistry.tools.put()`
2. **Add execution logic** in `McpToolExecutor.executeTool()`
3. **Add parameter extraction** helper method in `McpToolExecutor`

### Testing

The MCP server is tested as part of the main test suite:

```bash
./gradlew test
```

## Dependencies

The MCP server uses:
- Spring Boot 3.5.4
- Jackson (JSON processing)
- Lombok (code generation)
- Java 21+

## License

This MCP server implementation is part of the Points Back API project.

