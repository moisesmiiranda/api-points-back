package com.mmiranda.pointsbackapi.mcp.controller;

import com.mmiranda.pointsbackapi.mcp.model.McpRequest;
import com.mmiranda.pointsbackapi.mcp.model.McpResponse;
import com.mmiranda.pointsbackapi.mcp.tool.McpToolExecutor;
import com.mmiranda.pointsbackapi.mcp.tool.McpToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    @Autowired
    private McpToolRegistry toolRegistry;

    @Autowired
    private McpToolExecutor toolExecutor;

    @PostMapping("/rpc")
    public McpResponse handleRpc(@RequestBody McpRequest request) {
        try {
            switch (request.getMethod()) {
                case "tools/list":
                    return McpResponse.builder()
                        .jsonrpc("2.0")
                        .id(request.getId())
                        .result(Map.of(
                            "tools", toolRegistry.getAllTools()
                        ))
                        .build();

                case "tools/call":
                    String toolName = (String) request.getParams().get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) request.getParams().get("arguments");

                    if (params == null) {
                        params = new HashMap<>();
                    }

                    if (!toolRegistry.hasTool(toolName)) {
                        return buildErrorResponse(
                            request.getId(),
                            -32601,
                            "Method not found: " + toolName
                        );
                    }

                    Object result = toolExecutor.executeTool(toolName, params);
                    return McpResponse.builder()
                        .jsonrpc("2.0")
                        .id(request.getId())
                        .result(result)
                        .build();

                default:
                    return buildErrorResponse(
                        request.getId(),
                        -32601,
                        "Method not found: " + request.getMethod()
                    );
            }
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(
                request.getId(),
                -32602,
                "Invalid params: " + e.getMessage()
            );
        } catch (Exception e) {
            return buildErrorResponse(
                request.getId(),
                -32603,
                "Internal error: " + e.getMessage()
            );
        }
    }

    private McpResponse buildErrorResponse(String id, int code, String message) {
        return McpResponse.builder()
            .jsonrpc("2.0")
            .id(id)
            .error(com.mmiranda.pointsbackapi.mcp.model.McpError.builder()
                .code(code)
                .message(message)
                .build())
            .build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "message", "MCP Server is running");
    }
}


