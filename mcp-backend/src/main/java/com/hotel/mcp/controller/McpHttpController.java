package com.hotel.mcp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom MCP Controller that exposes Spring AI tools via synchronous HTTP
 * endpoint.
 * This provides a simpler JSON-RPC 2.0 interface for clients that don't support
 * SSE.
 */
@RestController
@RequestMapping("/mcp")
public class McpHttpController {

    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "hotel-mcp-server";
    private static final String SERVER_VERSION = "1.0.0";

    private final List<ToolCallbackProvider> toolCallbackProviders;
    private final ObjectMapper objectMapper;

    public McpHttpController(List<ToolCallbackProvider> toolCallbackProviders, ObjectMapper objectMapper) {
        this.toolCallbackProviders = toolCallbackProviders;
        this.objectMapper = objectMapper;
        log.info("McpHttpController initialized with {} tool callback providers", toolCallbackProviders.size());
    }

    @PostMapping(value = "/http", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handleRequest(@RequestBody Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        Object params = request.get("params");

        log.info("MCP HTTP Request: method={}, id={}", method, id);

        try {
            Object result = switch (method) {
                case "initialize" -> handleInitialize();
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolCall(params);
                case "ping" -> Map.of();
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };

            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.put("result", result);
            return response;

        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.put("error", Map.of(
                    "code", -32603,
                    "message", e.getMessage()));
            return response;
        }
    }

    private Map<String, Object> handleInitialize() {
        log.info("MCP Initialize request received");
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", false)),
                "serverInfo", Map.of(
                        "name", SERVER_NAME,
                        "version", SERVER_VERSION));
    }

    private Map<String, Object> handleToolsList() {
        log.info("MCP tools/list request received");
        List<Map<String, Object>> tools = new ArrayList<>();

        for (ToolCallbackProvider provider : toolCallbackProviders) {
            for (FunctionCallback callback : provider.getToolCallbacks()) {
                Map<String, Object> tool = new HashMap<>();
                tool.put("name", callback.getName());
                tool.put("description", callback.getDescription());
                tool.put("inputSchema", parseInputSchema(callback.getInputTypeSchema()));
                tools.add(tool);
                log.debug("Added tool: {}", callback.getName());
            }
        }

        log.info("Returning {} tools", tools.size());
        return Map.of("tools", tools);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInputSchema(String inputSchemaJson) {
        try {
            return objectMapper.readValue(inputSchemaJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse input schema: {}", e.getMessage());
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolCall(Object params) {
        log.info("MCP tools/call request received: {}", params);

        try {
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String toolName = (String) paramsMap.get("name");
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

            log.info("Calling tool: {} with arguments: {}", toolName, arguments);

            // Find the tool callback
            FunctionCallback targetCallback = null;
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                for (FunctionCallback callback : provider.getToolCallbacks()) {
                    if (callback.getName().equals(toolName)) {
                        targetCallback = callback;
                        break;
                    }
                }
                if (targetCallback != null)
                    break;
            }

            if (targetCallback == null) {
                throw new IllegalArgumentException("Unknown tool: " + toolName);
            }

            // Call the tool
            String argumentsJson = objectMapper.writeValueAsString(arguments != null ? arguments : Map.of());
            String resultJson = targetCallback.call(argumentsJson);

            log.info("Tool result: {}", resultJson);

            return Map.of(
                    "content", List.of(Map.of(
                            "type", "text",
                            "text", resultJson)),
                    "isError", false);

        } catch (Exception e) {
            log.error("Error executing tool", e);
            return Map.of(
                    "content", List.of(Map.of(
                            "type", "text",
                            "text", "Error: " + e.getMessage())),
                    "isError", true);
        }
    }
}
