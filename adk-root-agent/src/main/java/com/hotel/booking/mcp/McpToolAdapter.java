package com.hotel.booking.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;

import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Dynamic ADK Tool adapter that wraps MCP tools discovered from the MCP server.
 * This allows the agent to dynamically discover and use any tools exposed by
 * the MCP backend
 * without needing explicit wrapper classes for each tool.
 */
@Slf4j
public class McpToolAdapter extends BaseTool {

    private final McpClient mcpClient;
    private final String toolName;
    private final String toolDescription;
    private final Map<String, Object> inputSchema;

    /**
     * Create an MCP tool adapter from a tool definition returned by the MCP server
     */
    @SuppressWarnings("unchecked")
    public McpToolAdapter(McpClient mcpClient, Map<String, Object> toolDefinition) {
        super(
                (String) toolDefinition.get("name"),
                (String) toolDefinition.get("description"));
        this.mcpClient = mcpClient;
        this.toolName = (String) toolDefinition.get("name");
        this.toolDescription = (String) toolDefinition.get("description");
        this.inputSchema = (Map<String, Object>) toolDefinition.get("inputSchema");
    }

    /**
     * Discover all tools from the MCP server and create adapters for them.
     * Note: Retry logic is handled by ToolDiscoveryService.
     */
    public static List<BaseTool> discoverTools(McpClient mcpClient) {
        List<BaseTool> tools = new ArrayList<>();

        try {
            // Initialize connection
            mcpClient.initialize();

            // List available tools
            List<Map<String, Object>> mcpTools = mcpClient.listTools();

            log.info("*** Discovered {} MCP tools from server", mcpTools.size());

            for (Map<String, Object> toolDef : mcpTools) {
                String name = (String) toolDef.get("name");
                log.info("*** Creating adapter for MCP tool: {}", name);
                tools.add(new McpToolAdapter(mcpClient, toolDef));
            }

        } catch (Exception e) {
            log.error("Failed to discover MCP tools: {}", e.getMessage());
            throw new RuntimeException("MCP tool discovery failed", e);
        }

        return tools;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<FunctionDeclaration> declaration() {
        try {
            Schema.Builder parametersBuilder = Schema.builder().type(Known.OBJECT);

            if (inputSchema != null) {
                Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
                List<String> required = (List<String>) inputSchema.get("required");

                if (properties != null) {
                    Map<String, Schema> schemaProperties = new HashMap<>();

                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                        String type = (String) propDef.get("type");
                        String description = (String) propDef.get("description");

                        Schema.Builder propBuilder = Schema.builder()
                                .description(description != null ? description : "");

                        // Map MCP types to Gemini types
                        switch (type != null ? type : "string") {
                            case "number":
                            case "integer":
                                propBuilder.type(Known.NUMBER);
                                break;
                            case "boolean":
                                propBuilder.type(Known.BOOLEAN);
                                break;
                            case "array":
                                propBuilder.type(Known.ARRAY);
                                break;
                            default:
                                propBuilder.type(Known.STRING);
                        }

                        schemaProperties.put(entry.getKey(), propBuilder.build());
                    }

                    parametersBuilder.properties(schemaProperties);
                }

                if (required != null && !required.isEmpty()) {
                    parametersBuilder.required(required);
                }
            }

            return Optional.of(FunctionDeclaration.builder()
                    .name(toolName)
                    .description(toolDescription)
                    .parameters(parametersBuilder.build())
                    .build());

        } catch (Exception e) {
            log.error("Error building function declaration for tool: {}", toolName, e);
            return Optional.empty();
        }
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> parameters, ToolContext context) {
        return Single.fromCallable(() -> {
            try {
                log.info("*** Calling MCP tool '{}' with parameters: {}", toolName, parameters);

                // Call the MCP server
                Map<String, Object> result = mcpClient.callTool(toolName, parameters);

                if (result == null || result.containsKey("error")) {
                    log.error("MCP server error for tool '{}': {}", toolName, result);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Failed to execute " + toolName);
                    return errorResult;
                }

                log.info("*** MCP tool '{}' returned successfully", toolName);

                // Return the raw result directly - the LLM will format it nicely
                return result;

            } catch (Exception e) {
                log.error("Error calling MCP tool '{}'", toolName, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                return errorResult;
            }
        });
    }

    /**
     * Format the MCP result as plain data for the LLM to process
     * Returns the raw data so the LLM can generate a natural language response
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> formatResultAsData(Map<String, Object> result) {
        // Return the raw result data - the LLM will format it nicely in natural
        // language
        // This ensures the user gets a conversational response, not raw JSON
        Map<String, Object> response = new HashMap<>();
        response.put("toolName", toolName);
        response.put("data", result);
        response.put("status", "success");
        return response;
    }

    /**
     * Format tool name into a display title
     */
    private String formatToolTitle(String name) {
        // Convert camelCase to Title Case with spaces
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                title.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                title.append(' ').append(c);
            } else {
                title.append(c);
            }
        }
        return title.toString() + " Results";
    }
}
