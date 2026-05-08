# MCP Server Configuration Guide

This guide shows how to configure and use the Points Back API MCP Server with different platforms and AI assistants.

## Prerequisites

- Points Back API running on `http://localhost:8080`
- MCP-compatible client/assistant installed

## Configuration by Platform

### Claude Desktop / Claude App

Claude supports MCP through the desktop application configuration.

#### 1. Create Configuration File

On **macOS/Linux**:
```bash
mkdir -p ~/.config/Claude
nano ~/.config/Claude/claude_desktop_config.json
```

On **Windows**:
```bash
mkdir %APPDATA%\Claude
notepad %APPDATA%\Claude\claude_desktop_config.json
```

#### 2. Add MCP Server Configuration

```json
{
  "mcpServers": {
    "points-back-api": {
      "command": "java",
      "args": ["-jar", "/path/to/api-points-back/build/libs/pointsbackapi-0.0.1-SNAPSHOT.jar"],
      "env": {
        "SERVER_PORT": "8080",
        "JAVA_TOOL_OPTIONS": "-Xmx512m"
      }
    }
  }
}
```

#### 3. Restart Claude

The MCP tools will be automatically available in Claude conversations.

#### 4. Usage Example

In Claude, you can now say:
```
Create a new customer named Maria Silva with email maria@example.com and phone 11987654321
```

Claude will:
1. Parse your request
2. Call the `create_client` MCP tool
3. Return the created customer data

### curl / Manual HTTP Requests

For testing or integration with custom applications:

```bash
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }'
```

### Node.js / JavaScript Client

```javascript
const axios = require('axios');

async function callMcpTool(toolName, args) {
  const response = await axios.post('http://localhost:8080/mcp/rpc', {
    jsonrpc: '2.0',
    id: 'js-client-1',
    method: 'tools/call',
    params: {
      name: toolName,
      arguments: args
    }
  });

  return response.data;
}

// Example: List all clients
callMcpTool('list_all_clients', {})
  .then(result => console.log(result))
  .catch(error => console.error(error));

// Example: Create a client
callMcpTool('create_client', {
  name: 'João Silva',
  email: 'joao@example.com',
  phone: '11988888888',
  cpf: '12345678901'
})
  .then(result => console.log(result))
  .catch(error => console.error(error));
```

### Python Client

```python
import requests
import json

class PointsBackMCPClient:
    def __init__(self, base_url='http://localhost:8080'):
        self.base_url = base_url
        self.request_id = 0

    def call_tool(self, tool_name: str, arguments: dict) -> dict:
        """Call an MCP tool"""
        self.request_id += 1
        
        payload = {
            'jsonrpc': '2.0',
            'id': str(self.request_id),
            'method': 'tools/call',
            'params': {
                'name': tool_name,
                'arguments': arguments
            }
        }

        response = requests.post(
            f'{self.base_url}/mcp/rpc',
            json=payload,
            headers={'Content-Type': 'application/json'}
        )

        return response.json()

    def list_clients(self) -> dict:
        """List all clients"""
        return self.call_tool('list_all_clients', {})

    def create_client(self, name: str, email: str, phone: str, cpf: str) -> dict:
        """Create a new client"""
        return self.call_tool('create_client', {
            'name': name,
            'email': email,
            'phone': phone,
            'cpf': cpf
        })

    def register_purchase(self, client_id: int, establishment_id: int, amount: float) -> dict:
        """Register a purchase"""
        return self.call_tool('create_purchase', {
            'client_id': client_id,
            'establishment_id': establishment_id,
            'amount': amount
        })

# Usage Example
client = PointsBackMCPClient()

# List all clients
clients = client.list_clients()
print(json.dumps(clients, indent=2))

# Create a new client
new_client = client.create_client(
    name='Alice Santos',
    email='alice@example.com',
    phone='11999999999',
    cpf='99999999999'
)
print(json.dumps(new_client, indent=2))

# Register a purchase
purchase = client.register_purchase(
    client_id=1,
    establishment_id=1,
    amount=150.50
)
print(json.dumps(purchase, indent=2))
```

### Docker Deployment

For containerized deployment:

#### Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/pointsbackapi-*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### docker-compose.yml
```yaml
version: '3.8'

services:
  points-back-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_APPLICATION_NAME=pointsbackapi
      - SERVER_PORT=8080
    volumes:
      - ./data:/data

  # Optional: Add a GUI client
  mcp-client:
    image: node:18-alpine
    working_dir: /app
    volumes:
      - .:/app
    command: npm install && npm start
```

#### Run with Docker
```bash
docker-compose up
```

### Postman / API Testing Tools

#### 1. Import as API

In Postman, create a new request:

**Method**: POST  
**URL**: `http://localhost:8080/mcp/rpc`  
**Headers**:
```
Content-Type: application/json
```

#### 2. Body Examples

**List Clients**:
```json
{
  "jsonrpc": "2.0",
  "id": "postman-1",
  "method": "tools/call",
  "params": {
    "name": "list_all_clients",
    "arguments": {}
  }
}
```

**Create Client**:
```json
{
  "jsonrpc": "2.0",
  "id": "postman-2",
  "method": "tools/call",
  "params": {
    "name": "create_client",
    "arguments": {
      "name": "Test User",
      "email": "test@example.com",
      "phone": "11999999999",
      "cpf": "11111111111"
    }
  }
}
```

### GitHub Actions / CI-CD

Integrate MCP testing into your CI/CD pipeline:

```yaml
name: Test MCP Server

on: [push, pull_request]

jobs:
  test-mcp:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build application
        run: ./gradlew build -x test
      
      - name: Start application
        run: ./gradlew bootRun > /tmp/app.log 2>&1 &
      
      - name: Wait for startup
        run: sleep 10
      
      - name: Test MCP health endpoint
        run: curl -f http://localhost:8080/mcp/health
      
      - name: Test list clients
        run: |
          curl -X POST http://localhost:8080/mcp/rpc \
            -H "Content-Type: application/json" \
            -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

### Slack Bot Integration

```javascript
const bolt = require('@slack/bolt');
const axios = require('axios');

const app = new bolt.App({ token: process.env.SLACK_BOT_TOKEN });

app.message('points', async ({ message, say }) => {
  try {
    const response = await axios.post('http://localhost:8080/mcp/rpc', {
      jsonrpc: '2.0',
      id: 'slack-' + Date.now(),
      method: 'tools/call',
      params: {
        name: 'list_all_clients',
        arguments: {}
      }
    });

    const clients = response.data.result;
    const text = clients
      .map(c => `• ${c.name} - ${c.points} points`)
      .join('\n');

    await say(`Total Clients:\n${text}`);
  } catch (error) {
    await say('Error fetching clients');
  }
});
```

## Environment Variables

Configure via environment or application.yml:

```bash
# Port configuration
SERVER_PORT=8080

# Database
SPRING_DATASOURCE_URL=jdbc:h2:mem:pointsbackdb
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_MMIRANDA=DEBUG
```

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process (on macOS/Linux)
kill -9 <PID>

# Use different port
SERVER_PORT=8081 ./gradlew bootRun
```

### Connection Refused
```bash
# Check if application is running
curl http://localhost:8080/mcp/health

# Check logs
tail -f /tmp/app.log
```

### Tool Not Found Error
```bash
# List all available tools
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

### JSON Parse Errors
- Ensure Content-Type header is `application/json`
- Validate JSON syntax in request body
- Check that all required parameters are provided

## Performance Tuning

### JVM Options
```bash
export JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC"
./gradlew bootRun
```

### Database Connection Pool
```yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

## Security Best Practices

1. **Use HTTPS in Production**
   ```yml
   server:
     ssl:
       key-store: classpath:keystore.p12
       key-store-password: ${SSL_KEYSTORE_PASSWORD}
   ```

2. **API Authentication** (if needed)
   ```java
   // Add to McpController if required
   @PostMapping("/rpc")
   public McpResponse handleRpc(
       @RequestHeader("Authorization") String token,
       @RequestBody McpRequest request) {
       // Verify token before processing
   }
   ```

3. **Rate Limiting**
   ```gradle
   implementation("io.github.bucket4j:bucket4j-core:8.0.0")
   ```

## Resources

- [MCP Specification](https://modelcontextprotocol.io/)
- [Claude Documentation](https://claude.ai/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## Support

For issues or questions:
1. Check the MCP_QUICKSTART.md guide
2. Review MCP_SERVER.md for detailed API reference
3. Examine application logs for errors
4. Test with curl first before integrating

---

Happy coding! 🚀

