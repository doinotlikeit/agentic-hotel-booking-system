package com.hotel.booking.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.agent.ADKAgent;
import com.hotel.booking.model.AgentMessage;
import com.hotel.booking.model.AgentSessionState;
import com.hotel.booking.service.SessionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ADKAgent rootAgent;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public WebSocketHandler(ADKAgent rootAgent, SessionManager sessionManager) {
        this.rootAgent = rootAgent;
        this.sessionManager = sessionManager;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        activeSessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("Received message: {}", payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            String messageType = jsonNode.get("type").asText();

            // Handle ping/pong
            if ("ping".equals(messageType)) {
                handlePing(session);
                return;
            }

            // Handle chat messages
            if ("chat".equals(messageType)) {
                handleChatMessage(session, jsonNode);
            }

        } catch (Exception e) {
            log.error("Error handling message", e);
            sendError(session, "Error processing your message. Please try again.");
        }
    }

    private void handlePing(WebSocketSession session) throws IOException {
        log.debug("Received ping, sending pong");
        String pongResponse = "{\"type\":\"pong\"}";
        session.sendMessage(new TextMessage(pongResponse));
    }

    private void handleChatMessage(WebSocketSession session, JsonNode jsonNode) {
        try {
            JsonNode messageNode = jsonNode.get("message");

            String sessionId = messageNode.get("sessionId").asText();
            String appId = messageNode.get("appId").asText();
            String userId = messageNode.get("userId").asText();
            String content = messageNode.get("content").asText();

            // Get or create session state
            AgentSessionState state = sessionManager.getOrCreateSession(sessionId, appId, userId);

            // Add user message to history
            AgentMessage userMessage = objectMapper.treeToValue(messageNode, AgentMessage.class);
            state.addMessage(userMessage);

            // Detect if user wants the response read aloud and strip the command from the
            // message
            final boolean readAloud = detectReadAloudRequest(content);
            final String processedContent = readAloud ? stripReadAloudCommand(content) : content;
            log.info("User wants readAloud: {}, original: [{}], processed: [{}]", readAloud, content, processedContent);

            // Process message with ADK-based root agent
            rootAgent.processAsync(state, processedContent, responseText -> {
                try {
                    sendMessage(session, responseText, readAloud);
                } catch (IOException e) {
                    log.error("Error sending agent response to UI", e);
                }
            }).exceptionally(throwable -> {
                log.error("Error in agent processing", throwable);
                try {
                    sendError(session, "An error occurred while processing your request.");
                } catch (IOException e) {
                    log.error("Error sending error message", e);
                }
                return null;
            });

        } catch (Exception e) {
            log.error("Error handling chat message", e);
            try {
                sendError(session, "Error processing chat message.");
            } catch (IOException ioException) {
                log.error("Error sending error", ioException);
            }
        }
    }

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        sendMessage(session, message, false);
    }

    private void sendMessage(WebSocketSession session, String message, boolean readAloud) throws IOException {
        if (session.isOpen()) {
            log.info("=== sendMessage called (readAloud={}) ===", readAloud);
            log.info("Raw message: {}", message);

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("sessionId", session.getId());
            messageData.put("type", "agent");
            messageData.put("timestamp", java.time.Instant.now().toString());
            messageData.put("messageId", java.util.UUID.randomUUID().toString());

            // Try to parse message as JSON to check if it contains A2UI metadata
            // First, check if the message is wrapped in markdown code blocks
            String jsonContent = message;
            if (message.trim().startsWith("```json") || message.trim().startsWith("```")) {
                // Extract JSON from markdown code block
                String cleaned = message.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "");
                cleaned = cleaned.replaceAll("```\\s*$", "");
                jsonContent = cleaned.trim();
                log.info("Extracted JSON from markdown code block");
            }

            try {
                JsonNode jsonNode = objectMapper.readTree(jsonContent);
                log.info("Parsed JSON node: {}", jsonNode);
                log.info("Has 'a2ui' key: {}", jsonNode.has("a2ui"));
                log.info("Has 'components' key: {}", jsonNode.has("components"));
                log.info("Has 'format' key: {}", jsonNode.has("format"));

                boolean hasA2UIMetadata = false;
                Map<String, Object> dataObject = new HashMap<>();

                // Priority 1: Check for LLM-wrapped format: {"a2ui": {...}, "components":
                // [...]}
                // The LLM often wraps and adds root-level components array
                if (jsonNode.has("a2ui")) {
                    JsonNode a2uiNode = jsonNode.get("a2ui");

                    // Use root-level components if they exist, otherwise check inside a2ui node
                    JsonNode componentsNode = null;
                    if (jsonNode.has("components") && jsonNode.get("components").isArray()) {
                        componentsNode = jsonNode.get("components");
                        log.info("✅ Found components at root level");
                    } else if (a2uiNode.has("components")) {
                        componentsNode = a2uiNode.get("components");
                        log.info("✅ Found components inside a2ui node");
                    } else if (a2uiNode.has("elements")) {
                        componentsNode = a2uiNode.get("elements");
                        log.info("✅ Found elements inside a2ui node (converting to components)");
                    }

                    if (componentsNode != null) {
                        dataObject.put("format", "a2ui");
                        dataObject.put("components", objectMapper.convertValue(componentsNode, Object.class));
                        hasA2UIMetadata = true;
                        log.info("✅ Detected LLM-wrapped A2UI format");
                    }
                }
                // Priority 2: Check for direct A2UI format: {"format": "a2ui", "components":
                // [...]}
                else if (jsonNode.has("format") && "a2ui".equals(jsonNode.get("format").asText())
                        && jsonNode.has("components")) {
                    dataObject.put("format", "a2ui");
                    dataObject.put("components", objectMapper.convertValue(jsonNode.get("components"), Object.class));
                    hasA2UIMetadata = true;
                    log.info("✅ Detected direct A2UI format");
                }

                if (hasA2UIMetadata) {
                    messageData.put("content", ""); // Empty content since A2UI will render
                    messageData.put("data", dataObject);
                    log.info("Data object sent to frontend: {}", dataObject);
                } else {
                    // Regular text message
                    messageData.put("content", message);
                    log.info("Regular text message");
                }
            } catch (Exception e) {
                // Not valid JSON or doesn't contain A2UI - treat as regular text
                messageData.put("content", message);
                log.info("Not JSON or parse error, treating as text: {}", e.getMessage());
            }

            // Add readAloud flag if user requested it
            if (readAloud) {
                messageData.put("readAloud", true);
                log.info("✅ Added readAloud=true to response");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "chat");
            response.put("message", messageData);

            String json = objectMapper.writeValueAsString(response);
            log.info("Sending to frontend: {}", json);
            session.sendMessage(new TextMessage(json));
            log.info("*** Message sent to UI, sessionId: {}", session.getId());
        } else {
            log.warn("Cannot send message to UI - WebSocket session is closed");
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        if (session.isOpen()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "error");
            errorResponse.put("error", errorMessage);
            String json = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * Detect if user is requesting to read results aloud.
     * Matches phrases like "read results", "read it", "read that", "speak results",
     * etc.
     */
    private boolean detectReadAloudRequest(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase().trim();
        return lower.contains("read results") ||
                lower.contains("read result") ||
                lower.contains("read it") ||
                lower.contains("read that") ||
                lower.contains("read response") ||
                lower.contains("read the results") ||
                lower.contains("read out") ||
                lower.contains("read out loud") ||
                lower.contains("speak results") ||
                lower.contains("say it") ||
                lower.contains("read aloud") ||
                lower.contains("read this") ||
                lower.contains("and read") ||
                lower.contains("say the results") ||
                lower.contains("tell me the results");
    }

    /**
     * Strip read aloud commands from the user message.
     * E.g., "search hotels in Paris read results" -> "search hotels in Paris"
     */
    private String stripReadAloudCommand(String userMessage) {
        if (userMessage == null) {
            return "";
        }

        // List of phrases to strip (order matters - longer phrases first)
        String[] phrasesToStrip = {
                "and read the results",
                "read the results",
                "and read results",
                "read out loud",
                "tell me the results",
                "say the results",
                "speak results",
                "read response",
                "read results",
                "read result",
                "read aloud",
                "and read",
                "read out",
                "read this",
                "read that",
                "read it",
                "say it"
        };

        String result = userMessage;
        String lowerResult = result.toLowerCase();

        for (String phrase : phrasesToStrip) {
            int index = lowerResult.indexOf(phrase);
            if (index != -1) {
                // Remove the phrase (case-insensitive)
                result = result.substring(0, index) + result.substring(index + phrase.length());
                lowerResult = result.toLowerCase();
            }
        }

        // Clean up extra whitespace
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        activeSessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Suppress ClosedChannelException during shutdown - this is expected
        if (exception instanceof java.io.IOException &&
                exception.getCause() instanceof java.nio.channels.ClosedChannelException) {
            log.debug("WebSocket channel closed for session {}: {}", session.getId(), exception.getMessage());
        } else {
            log.error("WebSocket transport error for session: {}", session.getId(), exception);
        }
        activeSessions.remove(session.getId());
    }

    /**
     * Close all active WebSocket connections gracefully
     */
    public void closeAllConnections() {
        log.info("Closing {} active WebSocket connections", activeSessions.size());
        activeSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.GOING_AWAY);
                }
            } catch (java.io.IOException e) {
                // Suppress IOException during shutdown - channels may already be closed
                log.debug("IOException while closing WebSocket (expected during shutdown): {}", e.getMessage());
            } catch (Exception e) {
                log.debug("Error closing WebSocket session {}: {}", session.getId(), e.getMessage());
            }
        });
        activeSessions.clear();
    }
}
