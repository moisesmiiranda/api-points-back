package com.mmiranda.pointsbackapi.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";

    private String id;
    private Object result;
    private McpError error;
}


