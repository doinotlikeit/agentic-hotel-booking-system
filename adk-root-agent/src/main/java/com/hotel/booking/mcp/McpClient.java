package com.hotel.booking.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Client for communicating with Spring AI MCP Server.
 * Uses JSON-RPC 2.0 protocol over HTTP for tool discovery and execution.
 */
@Slf4j
@Component
public class McpClient {

    @Value("${mcp.server.url:http://localhost:8081}")
    private String mcpServerUrl;

    private RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int requestId = 0;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(mcpServerUrl)
                .build();
        log.info("MCP Client initialized with server URL: {}", mcpServerUrl);
    }

    /**
     * Initialize connection with MCP server
     */
    public Map<String, Object> initialize() {
        return callMethod("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                        "name", "hotel-agent-client",
                        "version", "1.0.0")));
    }

    /**
     * List available tools from MCP server
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listTools() {
        Map<String, Object> response = callMethod("tools/list", Map.of());
        if (response != null && response.containsKey("tools")) {
            return (List<Map<String, Object>>) response.get("tools");
        }
        return List.of();
    }

    /**
     * Call a tool on the MCP server
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : Map.of());

        Map<String, Object> response = callMethod("tools/call", params);

        if (response != null && response.containsKey("content")) {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (!content.isEmpty()) {
                Map<String, Object> firstContent = content.get(0);
                if ("text".equals(firstContent.get("type"))) {
                    String text = (String) firstContent.get("text");
                    try {
                        return objectMapper.readValue(text, Map.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse tool response as JSON: {}", e.getMessage());
                        return Map.of("text", text);
                    }
                }
            }
        }
        return response != null ? response : Map.of("error", "No response from MCP server");
    }

    /**
     * Call a JSON-RPC method on the MCP server
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callMethod(String method, Map<String, Object> params) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", ++requestId);
            request.put("method", method);
            request.put("params", params);

            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("MCP Request: {}", requestJson);

            String responseJson = restClient.post()
                    .uri("/mcp/http")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            log.debug("MCP Response: {}", responseJson);

            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

            if (response.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                log.error("MCP Error: {}", error);
                return null;
            }

            return (Map<String, Object>) response.get("result");

        } catch (Exception e) {
            log.error("Error calling MCP method {}: {}", method, e.getMessage());
            return null;
        }
    }
}
