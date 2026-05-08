# MCP Server Quick Start Guide

This guide will help you get started with the MCP (Model Context Protocol) server for the Points Back API.

## What is MCP?

The Model Context Protocol (MCP) is a standardized protocol that allows AI assistants and other clients to interact with your application through a set of defined tools. This enables natural language interfaces to your API.

## Installation & Setup

### Prerequisites
- Java 21+
- Gradle
- cURL or similar HTTP client for testing

### Building the Project

```bash
cd /home/devton/projects/api-points-back
chmod +x gradlew
./gradlew build
```

### Running the Application

```bash
./gradlew bootRun
```

The API will start on `http://localhost:8080` (check `application.yml` for port configuration).

### Verify MCP Server is Running

```bash
curl http://localhost:8080/mcp/health
```

Expected response:
```json
{"status":"ok","message":"MCP Server is running"}
```

## Available Tools

The MCP server exposes 13 tools organized into 3 categories:

### Client Management (5 tools)
- `list_all_clients` - List all customers
- `get_client` - Get a specific customer
- `create_client` - Create a new customer
- `update_client` - Update customer information
- `add_points_to_client` - Add loyalty points to a customer

### Establishment Management (4 tools)
- `list_all_establishments` - List all partner establishments
- `get_establishment` - Get a specific establishment
- `create_establishment` - Create a new establishment
- `update_establishment` - Update establishment information

### Purchase Management (4 tools)
- `list_all_purchases` - List all purchases
- `get_purchase` - Get a specific purchase
- `create_purchase` - Register a new purchase
- `update_purchase` - Update purchase information

## Example: List All Clients

### Using cURL

```bash
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/call",
    "params": {
      "name": "list_all_clients",
      "arguments": {}
    }
  }'
```

### Response

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": [
    {
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "11999999999",
      "cpf": "123.456.789-00",
      "points": 100
    }
  ]
}
```

## Example: Create a Client

```bash
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "create-1",
    "method": "tools/call",
    "params": {
      "name": "create_client",
      "arguments": {
        "name": "Maria Silva",
        "email": "maria@example.com",
        "phone": "11987654321",
        "cpf": "111.222.333-44"
      }
    }
  }'
```

## Example: Register a Purchase

```bash
curl -X POST http://localhost:8080/mcp/rpc \
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
        "amount": 150.50
      }
    }
  }'
```

## Example: List All Tools

Get the complete list of available tools and their parameters:

```bash
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "list-tools",
    "method": "tools/list",
    "params": {}
  }'
```

## Using with Claude

To integrate the MCP server with Claude, configure it in your Claude client configuration:

```json
{
  "mcpServers": {
    "points-api": {
      "command": "java",
      "args": ["-jar", "build/libs/pointsbackapi-0.0.1-SNAPSHOT.jar"]
    }
  }
}
```

Then you can use natural language commands like:
- "Create a new customer named Bob with email bob@example.com"
- "Show me all clients"
- "Add 100 points to client ID 1"
- "Register a purchase of $200 for client 1 at establishment 2"

## JSON-RPC 2.0 Format

All requests to the MCP server follow the JSON-RPC 2.0 specification:

### Request Structure
```json
{
  "jsonrpc": "2.0",
  "id": "unique-request-id",
  "method": "tools/call",
  "params": {
    "name": "tool_name",
    "arguments": { /* tool-specific arguments */ }
  }
}
```

### Response Structure (Success)
```json
{
  "jsonrpc": "2.0",
  "id": "unique-request-id",
  "result": { /* tool result */ }
}
```

### Response Structure (Error)
```json
{
  "jsonrpc": "2.0",
  "id": "unique-request-id",
  "error": {
    "code": -32602,
    "message": "Invalid params"
  }
}
```

## Error Codes

| Code | Meaning |
|------|---------|
| -32601 | Method not found |
| -32602 | Invalid parameters |
| -32603 | Internal error |

## Troubleshooting

### Application won't start
```bash
# Check if port 8080 is already in use
lsof -i :8080

# Start on a different port by modifying application.yml
```

### MCP endpoint returns 404
- Ensure the application is running
- Check that you're using `localhost:8080` (not 8081)
- Verify the request path is `/mcp/rpc`

### Tool execution fails
- Check the error message in the response
- Verify all required parameters are provided
- Ensure referenced IDs (client_id, establishment_id) exist

## Performance Tips

- The MCP server is built on Spring Boot and scales well
- Database queries are optimized with JPA
- All responses are JSON-serialized

## Additional Resources

- For detailed API documentation: See `MCP_SERVER.md`
- For REST API documentation: See `README.md`
- Application configuration: `src/main/resources/application.yml`

## Support

For issues or feature requests, check the project documentation or review the MCP implementation in:
- `/src/main/java/com/mmiranda/pointsbackapi/mcp/`

