package com.mmiranda.pointsbackapi.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mmiranda.pointsbackapi.mcp.model.McpTool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class McpToolRegistry {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, McpTool> tools = new HashMap<>();

    public McpToolRegistry() {
        registerTools();
    }

    private void registerTools() {
        // Client tools
        tools.put("list_all_clients", buildTool(
            "list_all_clients",
            "List all clients in the system",
            buildInputSchema(new HashMap<>())
        ));

        tools.put("get_client", buildTool(
            "get_client",
            "Get a specific client by ID",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The client ID")
            ))
        ));

        tools.put("create_client", buildTool(
            "create_client",
            "Create a new client",
            buildInputSchema(Map.of(
                "name", Map.of("type", "string", "description", "Client name"),
                "email", Map.of("type", "string", "description", "Client email"),
                "phone", Map.of("type", "string", "description", "Client phone"),
                "cpf", Map.of("type", "string", "description", "Client CPF"),
                "points", Map.of("type", "number", "description", "Initial points (optional)")
            ))
        ));

        tools.put("update_client", buildTool(
            "update_client",
            "Update client information",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The client ID"),
                "name", Map.of("type", "string", "description", "Client name (optional)"),
                "email", Map.of("type", "string", "description", "Client email (optional)"),
                "phone", Map.of("type", "string", "description", "Client phone (optional)"),
                "cpf", Map.of("type", "string", "description", "Client CPF (optional)"),
                "points", Map.of("type", "number", "description", "Client points (optional)")
            ))
        ));

        tools.put("add_points_to_client", buildTool(
            "add_points_to_client",
            "Add points to a client's account",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The client ID"),
                "points", Map.of("type", "number", "description", "Points to add")
            ))
        ));

        // Establishment tools
        tools.put("list_all_establishments", buildTool(
            "list_all_establishments",
            "List all establishments in the system",
            buildInputSchema(new HashMap<>())
        ));

        tools.put("get_establishment", buildTool(
            "get_establishment",
            "Get a specific establishment by ID",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The establishment ID")
            ))
        ));

        tools.put("create_establishment", buildTool(
            "create_establishment",
            "Create a new establishment",
            buildInputSchema(Map.of(
                "name", Map.of("type", "string", "description", "Establishment name"),
                "email", Map.of("type", "string", "description", "Establishment email"),
                "phone", Map.of("type", "string", "description", "Establishment phone"),
                "cnpj", Map.of("type", "string", "description", "Establishment CNPJ"),
                "valuePerPoint", Map.of("type", "number", "description", "Value per point (optional)")
            ))
        ));

        tools.put("update_establishment", buildTool(
            "update_establishment",
            "Update establishment information",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The establishment ID"),
                "name", Map.of("type", "string", "description", "Establishment name (optional)"),
                "email", Map.of("type", "string", "description", "Establishment email (optional)"),
                "phone", Map.of("type", "string", "description", "Establishment phone (optional)"),
                "cnpj", Map.of("type", "string", "description", "Establishment CNPJ (optional)"),
                "valuePerPoint", Map.of("type", "number", "description", "Value per point (optional)")
            ))
        ));

        // Purchase tools
        tools.put("list_all_purchases", buildTool(
            "list_all_purchases",
            "List all purchases in the system",
            buildInputSchema(new HashMap<>())
        ));

        tools.put("get_purchase", buildTool(
            "get_purchase",
            "Get a specific purchase by ID",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The purchase ID")
            ))
        ));

        tools.put("create_purchase", buildTool(
            "create_purchase",
            "Create a new purchase",
            buildInputSchema(Map.of(
                "client_id", Map.of("type", "number", "description", "The client ID"),
                "establishment_id", Map.of("type", "number", "description", "The establishment ID"),
                "amount", Map.of("type", "number", "description", "Purchase amount")
            ))
        ));

        tools.put("update_purchase", buildTool(
            "update_purchase",
            "Update purchase information",
            buildInputSchema(Map.of(
                "id", Map.of("type", "number", "description", "The purchase ID"),
                "client_id", Map.of("type", "number", "description", "The client ID (optional)"),
                "establishment_id", Map.of("type", "number", "description", "The establishment ID (optional)"),
                "amount", Map.of("type", "number", "description", "Purchase amount (optional)")
            ))
        ));
    }

    private McpTool buildTool(String name, String description, Object inputSchema) {
        return McpTool.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .build();
    }

    private Object buildInputSchema(Map<String, Object> properties) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode props = objectMapper.createObjectNode();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            props.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
        }
        schema.set("properties", props);

        return schema;
    }

    public List<McpTool> getAllTools() {
        return tools.values().stream().toList();
    }

    public McpTool getTool(String name) {
        return tools.get(name);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}



