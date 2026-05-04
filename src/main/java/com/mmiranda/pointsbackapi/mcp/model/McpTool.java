package com.mmiranda.pointsbackapi.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String name;
    private String description;
    private Object inputSchema;
}

