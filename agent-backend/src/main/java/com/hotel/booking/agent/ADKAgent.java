package com.hotel.booking.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.Gemini;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.tools.BaseTool;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hotel.booking.mcp.McpClient;
import com.hotel.booking.mcp.McpToolAdapter;
import com.hotel.booking.model.AgentMessage;
import com.hotel.booking.model.AgentSessionState;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Root agent implementation using Google ADK SDK.
 * Integrates with Gemini model via Vertex AI for intelligent hotel booking
 * assistance.
 */
@Slf4j
@Component
public class ADKAgent {

    @Value("${gcp.vertex.model}")
    private String modelName;

    @Value("${gcp.project.id}")
    private String projectId;

    @Value("${gcp.location}")
    private String location;

    @Value("${agent.name}")
    private String agentName;

    @Value("${agent.description}")
    private String agentDescription;

    private BaseAgent rooAgent;

    private final McpClient mcpClient;

    public ADKAgent(McpClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    @PostConstruct
    public void init() {
        createAgent();
        log.info("*** Intialized ...");
    }

    /**
     * Initialize the hotel booking agent with tools
     */
    private void createAgent() {
        log.info("*** Creating agent: {} with model: {}, project: {}, location: {}", agentName, modelName, projectId,
                location);

        // Discover tools from MCP server dynamically
        List<BaseTool> mcpTools = McpToolAdapter.discoverTools(mcpClient);
        log.info("*** Discovered {} tools from MCP server", mcpTools.size());

        BaseAgent agent = LlmAgent.builder()
                .name(agentName)
                .description(agentDescription)
                .instruction(
                        """
                                You are a helpful hotel booking assistant. Help users search for and book hotels.

                                ⚠️ CRITICAL RULES - MUST FOLLOW ⚠️:
                                1. When users ask to search for hotels, you MUST ALWAYS call the searchHotels tool FIRST
                                2. When users ask for prices, you MUST ALWAYS call the getHotelPrice tool FIRST
                                3. When users want to book, you MUST ALWAYS call the bookHotel tool FIRST
                                4. NEVER EVER respond with text before calling the appropriate tool
                                5. NEVER invent, make up, or create your own hotel data - ALWAYS use tools
                                6. DO NOT return JSON manually - the tools return properly formatted data
                                7. If a user asks "show hotels in [city]" or similar, your FIRST action is to call searchHotels, NOT to respond with text

                                Tool Usage:
                                - searchHotels: Search for hotels (required: destination, optional: minRating, maxPrice)
                                - bookHotel: Book a room (required: hotelName, checkInDate, checkOutDate, guestName)
                                - getHotelPrice: Get pricing (required: hotelName, numberOfNights)

                                REMEMBER: Always call the tool FIRST, then respond based on the tool's output!
                                """)
                .model(new Gemini(modelName,
                        Client.builder()
                                .vertexAI(true)
                                .build()))
                .tools(mcpTools)
                .build();

        log.info("*** LlmAgent: [{}] built successfully with {} tools", agent.name(), mcpTools.size());
        this.rooAgent = agent;
    }

    public CompletableFuture<Void> processAsync(AgentSessionState sessionState, String userMessage,
            Consumer<String> messageConsumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {

            log.info("*** Processing client message: [{}] with sessionId: [{}] {}", userMessage,
                    sessionState.getSessionId());

            // Add message to be sent to user to history
            AgentMessage userMsg = AgentMessage.builder()
                    .sessionId(sessionState.getSessionId())
                    .appId(sessionState.getAppId())
                    .userId(sessionState.getUserId())
                    .messageId(java.util.UUID.randomUUID().toString())
                    .timestamp(java.time.Instant.now().toString())
                    .content(userMessage)
                    .type("user")
                    .build();
            sessionState.addMessage(userMsg);

            // Create Content for ADK
            Content userContent = Content.builder()
                    .role("user")
                    .parts(Part.builder().text(userMessage).build())
                    .build();

            // Get or create session in ADK's session service
            String userId = sessionState.getUserId();
            String sessionId = sessionState.getSessionId();

            InMemorySessionService sessionService = new InMemorySessionService();
            Runner runner = Runner.builder()
                    .appName(sessionState.getAppId())
                    .agent(rooAgent)
                    .sessionService(sessionService)
                    .build();
            com.google.adk.sessions.Session adkSession = sessionService.createSession(
                    sessionState.getAppId(),
                    userId,
                    null,
                    sessionId).blockingGet();

            log.info("*** ADK Session created: {}", adkSession);

            // Collect response text
            StringBuilder fullResponse = new StringBuilder();

            // Detect if user explicitly requested JSON format
            final boolean userRequestedJson = userMessage.toLowerCase()
                    .matches(".*\\b(json|in json|as json|show.*json)\\b.*");
            log.info("*** User requested JSON format: {}", userRequestedJson);

            // Flag to suppress text response when A2UI tree is sent
            final boolean[] suppressTextResponse = { false };

            // Run ADK agent using subscribe pattern
            log.info("*** Running agent asynchronously for user: {}, sessionId: {}", userId, sessionId);

            runner.runAsync(userId, adkSession.id(), userContent).subscribe(
                    event -> {
                        // onNext: process each event
                        log.info("Event ID: {}, Author: {}", event.id(), event.author());

                        boolean hasSpecificPart = false;
                        if (event.content().isPresent() && event.content().get().parts().isPresent()) {
                            for (Part part : event.content().get().parts().get()) {
                                if (part.executableCode().isPresent()) {
                                    log.info("  Debug: Agent generated code:\n```\n{}\n```",
                                            part.executableCode().get().code());
                                    hasSpecificPart = true;
                                } else if (part.codeExecutionResult().isPresent()) {
                                    log.info("  Debug: Code Execution Result: {} - Output:\n{}",
                                            part.codeExecutionResult().get().outcome(),
                                            part.codeExecutionResult().get().output());
                                    hasSpecificPart = true;
                                } else if (part.functionResponse().isPresent()) {
                                    // Tool response - check if it contains A2UI metadata
                                    var funcResponse = part.functionResponse().get();
                                    log.info("  Debug: Function Response from tool [{}]", funcResponse.name());
                                    if (funcResponse.response().isPresent()) {
                                        try {
                                            Map<String, Object> responseMap = funcResponse.response().get();
                                            log.info("  Debug: Tool response map: {}", responseMap);

                                            // Check if tool wants A2UI sent directly
                                            if (responseMap.containsKey("__a2ui_direct__") &&
                                                    Boolean.TRUE.equals(responseMap.get("__a2ui_direct__"))) {

                                                // Only send A2UI if user explicitly requested JSON format
                                                if (userRequestedJson) {
                                                    // Remove the marker
                                                    Map<String, Object> a2uiData = new HashMap<>(responseMap);
                                                    a2uiData.remove("__a2ui_direct__");

                                                    ObjectMapper mapper = new ObjectMapper();
                                                    String a2uiJson = mapper.writeValueAsString(a2uiData);
                                                    log.info("*** Sending JSON tree A2UI to frontend: {}", a2uiJson);
                                                    messageConsumer.accept(a2uiJson);
                                                    hasSpecificPart = true;

                                                    // Suppress subsequent text response from LLM
                                                    suppressTextResponse[0] = true;
                                                    log.info(
                                                            "*** Suppressing LLM text response since A2UI tree was sent");
                                                } else {
                                                    log.info(
                                                            "*** User did not request JSON - letting LLM format the response naturally");
                                                }
                                            }
                                            // Check if this is A2UI formatted response (legacy path)
                                            else if (responseMap.containsKey("a2ui")
                                                    && responseMap.containsKey("components")) {
                                                // Convert to JSON and send to frontend
                                                ObjectMapper mapper = new ObjectMapper();
                                                String a2uiJson = mapper.writeValueAsString(responseMap);
                                                log.info(
                                                        "  *** Detected A2UI response from tool, sending to frontend: {}",
                                                        a2uiJson);
                                                messageConsumer.accept(a2uiJson);
                                                hasSpecificPart = true;
                                            }
                                        } catch (Exception e) {
                                            log.error("Error processing function response", e);
                                        }
                                    }
                                } else if (part.text().isPresent() && !part.text().get().trim().isEmpty()) {
                                    String text = part.text().get().trim();
                                    log.info("*** Text response from LLM: [{}]", text);
                                    fullResponse.append(text);

                                    // Only send text if we haven't already sent A2UI tree
                                    if (!suppressTextResponse[0]) {
                                        // Wrap text in A2UI format before sending to client
                                        try {
                                            Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder
                                                    .wrapText(text);
                                            ObjectMapper mapper = new ObjectMapper();
                                            String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                                            log.info("*** Wrapped text in A2UI format: {}", a2uiJson);
                                            messageConsumer.accept(a2uiJson);
                                        } catch (Exception e) {
                                            log.error("Error wrapping text in A2UI", e);
                                            // Fallback to plain text
                                            messageConsumer.accept(text);
                                        }
                                    } else {
                                        log.info("*** Skipping LLM text response - A2UI tree already sent");
                                    }
                                }
                            }
                        }

                        if (!hasSpecificPart && event.finalResponse()) {
                            if (event.content().isPresent()
                                    && event.content().get().parts().isPresent()
                                    && !event.content().get().parts().get().isEmpty()
                                    && event.content().get().parts().get().getFirst().text().isPresent()) {
                                String finalResponse = event.content().get().parts().get().getFirst().text().get()
                                        .trim();
                                log.info("*** Final Agent Response: [{}]", finalResponse);
                            } else {
                                log.info("*** Final Agent Response: [No text content in final event]");
                            }
                        }
                    },
                    throwable -> {
                        // onError: handle errors
                        log.error("ERROR during agent run: {}", throwable.getMessage(), throwable);
                        String errorMsg = "I apologize, but I encountered an error: " + throwable.getMessage();

                        // Wrap error in A2UI format
                        try {
                            Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder.wrapText(errorMsg);
                            ObjectMapper mapper = new ObjectMapper();
                            String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                            messageConsumer.accept(a2uiJson);
                        } catch (Exception e) {
                            log.error("Error wrapping error message in A2UI", e);
                            messageConsumer.accept(errorMsg);
                        }

                        future.completeExceptionally(throwable);
                    },
                    () -> {
                        // onComplete: finalize when stream completes. Save the LLM response
                        String responseText = fullResponse.toString();
                        if (!responseText.isEmpty()) {
                            AgentMessage agentMsg = AgentMessage.createAgentMessage(
                                    sessionState.getSessionId(),
                                    sessionState.getAppId(),
                                    sessionState.getUserId(),
                                    responseText);
                            sessionState.addMessage(agentMsg);
                            log.info("*** Agent processing completed. Response length: {} chars",
                                    responseText.length());
                        } else {
                            log.warn("*** Agent completed but no response text was generated");
                            String errorMsg = "I apologize, but the AI agent did not generate a response. " +
                                    "This may be due to a configuration issue with the Gemini model or Vertex AI credentials. "
                                    +
                                    "Please check the server logs for more details.";

                            // Wrap error in A2UI format
                            try {
                                Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder
                                        .wrapText(errorMsg);
                                ObjectMapper mapper = new ObjectMapper();
                                String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                                messageConsumer.accept(a2uiJson);
                            } catch (Exception e) {
                                log.error("Error wrapping error message in A2UI", e);
                                messageConsumer.accept(errorMsg);
                            }
                        }
                        future.complete(null);
                    });

        } catch (Exception e) {
            log.error("Error starting ADK agent processing", e);

            String errorMsg = "I apologize, but I encountered an error starting the agent.";
            // Wrap error in A2UI format
            try {
                Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder.wrapText(errorMsg);
                ObjectMapper mapper = new ObjectMapper();
                String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                messageConsumer.accept(a2uiJson);
            } catch (Exception ex) {
                log.error("Error wrapping error message in A2UI", ex);
                messageConsumer.accept(errorMsg);
            }

            future.completeExceptionally(e);
        }

        return future;
    }
}
