package com.hotel.booking.a2a;

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
 * Dynamic ADK Tool adapter that wraps A2A agent skills.
 * This allows the agent to dynamically discover and use skills from A2A agents.
 */
@Slf4j
public class A2AToolAdapter extends BaseTool {

    private final A2AClient a2aClient;
    private final String skillId;
    private final String skillName;
    private final String skillDescription;
    private final Map<String, Object> inputSchema;

    /**
     * Create an A2A tool adapter from a skill definition
     */
    @SuppressWarnings("unchecked")
    public A2AToolAdapter(A2AClient a2aClient, Map<String, Object> skillDefinition) {
        super(
                skillIdToToolName((String) skillDefinition.get("id")),
                (String) skillDefinition.get("description"));
        this.a2aClient = a2aClient;
        this.skillId = (String) skillDefinition.get("id");
        this.skillName = (String) skillDefinition.get("name");
        this.skillDescription = (String) skillDefinition.get("description");
        this.inputSchema = (Map<String, Object>) skillDefinition.get("inputSchema");
    }

    /**
     * Convert skill ID to a valid tool name (e.g., "book-hotel" -> "bookHotel")
     */
    private static String skillIdToToolName(String skillId) {
        if (skillId == null)
            return "unknownSkill";

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : skillId.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Discover all skills from an A2A agent and create adapters for them.
     * Note: Retry logic is handled by ToolDiscoveryService.
     */
    public static List<BaseTool> discoverTools(A2AClient a2aClient) {
        List<BaseTool> tools = new ArrayList<>();

        try {
            // Discover the agent
            Map<String, Object> agentCard = a2aClient.discoverAgent();

            if (agentCard == null) {
                throw new RuntimeException("A2A agent not found");
            }

            // Get skills
            List<Map<String, Object>> skills = a2aClient.getSkills();

            log.info("*** Discovered {} A2A skills from agent '{}'",
                    skills.size(), agentCard.get("name"));

            for (Map<String, Object> skill : skills) {
                String id = (String) skill.get("id");
                String name = (String) skill.get("name");
                log.info("*** Creating adapter for A2A skill: {} ({})", name, id);
                tools.add(new A2AToolAdapter(a2aClient, skill));
            }

        } catch (Exception e) {
            log.error("Failed to discover A2A skills: {}", e.getMessage());
            throw new RuntimeException("A2A skill discovery failed", e);
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

                        // Map JSON Schema types to Gemini types
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
                    .name(name())
                    .description(skillDescription)
                    .parameters(parametersBuilder.build())
                    .build());

        } catch (Exception e) {
            log.error("Error building function declaration for A2A skill: {}", skillId, e);
            return Optional.empty();
        }
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> parameters, ToolContext context) {
        return Single.fromCallable(() -> {
            try {
                log.info("*** Calling A2A skill '{}' ({}) with parameters: {}", skillName, skillId, parameters);

                // Send task to A2A agent
                Map<String, Object> result = a2aClient.sendTask(skillId, parameters);

                if (result == null || result.containsKey("error")) {
                    log.error("A2A agent error for skill '{}': {}", skillId, result);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Failed to execute " + skillName);
                    return errorResult;
                }

                log.info("*** A2A skill '{}' returned successfully", skillName);

                // Return the raw result directly - the LLM will format it nicely
                return result;

            } catch (Exception e) {
                log.error("Error calling A2A skill '{}'", skillId, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                return errorResult;
            }
        });
    }
}
