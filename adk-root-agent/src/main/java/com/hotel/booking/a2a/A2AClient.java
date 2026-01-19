package com.hotel.booking.a2a;

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
 * A2A Client for communicating with A2A-enabled agents.
 * Implements the Agent-to-Agent protocol for discovering and invoking remote
 * agents.
 */
@Slf4j
@Component
public class A2AClient {

    @Value("${a2a.booking.url:http://localhost:8082}")
    private String a2aServerUrl;

    private RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int requestId = 0;

    // Cached agent card
    private Map<String, Object> agentCard;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(a2aServerUrl)
                .build();
        log.info("A2A Client initialized with server URL: {}", a2aServerUrl);
    }

    /**
     * Discover the agent by fetching its Agent Card
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> discoverAgent() {
        try {
            String responseJson = restClient.get()
                    .uri("/.well-known/agent.json")
                    .retrieve()
                    .body(String.class);

            log.debug("A2A Agent Card: {}", responseJson);

            this.agentCard = objectMapper.readValue(responseJson, Map.class);
            log.info("Discovered A2A agent: {} with {} skills",
                    agentCard.get("name"),
                    ((List<?>) agentCard.get("skills")).size());

            return agentCard;

        } catch (Exception e) {
            log.error("Error discovering A2A agent: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the cached agent card, or discover if not cached
     */
    public Map<String, Object> getAgentCard() {
        if (agentCard == null) {
            discoverAgent();
        }
        return agentCard;
    }

    /**
     * Get the list of skills from the agent card
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSkills() {
        Map<String, Object> card = getAgentCard();
        if (card != null && card.containsKey("skills")) {
            return (List<Map<String, Object>>) card.get("skills");
        }
        return List.of();
    }

    /**
     * Send a task to the A2A agent
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendTask(String skillId, Map<String, Object> parameters) {
        try {
            // Build message with data part containing the parameters
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("parts", List.of(
                    Map.of(
                            "type", "data",
                            "data", parameters,
                            "mimeType", "application/json")));

            // Build task params
            Map<String, Object> params = new HashMap<>();
            params.put("message", message);
            params.put("skillId", skillId);

            // Make JSON-RPC call
            Map<String, Object> response = callMethod("tasks/send", params);

            if (response != null) {
                log.info("A2A task completed: {}", response.get("id"));

                // Extract the result from artifacts
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.get("artifacts");
                if (artifacts != null && !artifacts.isEmpty()) {
                    Map<String, Object> artifact = artifacts.get(0);
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) artifact.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<String, Object> part = parts.get(0);
                        if ("data".equals(part.get("type"))) {
                            return (Map<String, Object>) part.get("data");
                        }
                    }
                }
            }

            return response;

        } catch (Exception e) {
            log.error("Error sending A2A task: {}", e.getMessage());
            return Map.of("error", "Failed to send task: " + e.getMessage());
        }
    }

    /**
     * Get the status of a task
     */
    public Map<String, Object> getTask(String taskId) {
        return callMethod("tasks/get", Map.of("id", taskId));
    }

    /**
     * Cancel a task
     */
    public Map<String, Object> cancelTask(String taskId) {
        return callMethod("tasks/cancel", Map.of("id", taskId));
    }

    /**
     * Call a JSON-RPC method on the A2A server
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
            log.debug("A2A Request: {}", requestJson);

            String responseJson = restClient.post()
                    .uri("/a2a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            log.debug("A2A Response: {}", responseJson);

            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

            // Check if there's an actual error (not just a null error field)
            Object errorObj = response.get("error");
            if (errorObj != null) {
                Map<String, Object> error = (Map<String, Object>) errorObj;
                log.error("A2A Error: {}", error);
                String errorMessage = error.get("message") != null ? error.get("message").toString() : "Unknown error";
                return Map.of("error", errorMessage);
            }

            return (Map<String, Object>) response.get("result");

        } catch (Exception e) {
            log.error("Error calling A2A method {}: {}", method, e.getMessage());
            return null;
        }
    }

    /**
     * Check if the A2A server is available
     */
    public boolean isAvailable() {
        try {
            Map<String, Object> card = discoverAgent();
            return card != null;
        } catch (Exception e) {
            return false;
        }
    }
}
