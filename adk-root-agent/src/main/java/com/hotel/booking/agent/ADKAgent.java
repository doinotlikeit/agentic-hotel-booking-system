package com.hotel.booking.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
import com.hotel.booking.model.AgentMessage;
import com.hotel.booking.model.AgentSessionState;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Root agent implementation using Google ADK SDK.
 * Integrates with Gemini model via Vertex AI for intelligent hotel booking
 * assistance.
 * Discovers tools dynamically from both MCP servers and A2A agents.
 */
@Slf4j
@Component
public class ADKAgent implements ToolDiscoveryService.ToolDiscoveryListener {

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

    // Atomic reference for thread-safe agent updates
    private final AtomicReference<BaseAgent> rootAgent = new AtomicReference<>();

    private final ToolDiscoveryService toolDiscoveryService;

    public ADKAgent(ToolDiscoveryService toolDiscoveryService) {
        this.toolDiscoveryService = toolDiscoveryService;
    }

    @PostConstruct
    public void init() {
        // Register as listener for tool discovery events
        toolDiscoveryService.addListener(this);

        // Create initial agent (may have no tools yet)
        rebuildAgent();
        log.info("*** ADKAgent initialized - waiting for tool discovery...");
    }

    /**
     * Called when tools are discovered by the ToolDiscoveryService
     */
    @Override
    public void onToolsDiscovered(String source, List<BaseTool> tools) {
        log.info("*** Tools discovered from {}: {} tools", source, tools.size());
        rebuildAgent();
    }

    /**
     * Rebuild the agent with currently available tools
     */
    private synchronized void rebuildAgent() {
        List<BaseTool> allTools = toolDiscoveryService.getAllTools();

        log.info("*** Rebuilding agent with {} tools (MCP: {}, A2A: {})",
                allTools.size(),
                toolDiscoveryService.getMcpTools().size(),
                toolDiscoveryService.getA2aTools().size());

        try {
            BaseAgent agent = LlmAgent.builder()
                    .name(agentName)
                    .description(agentDescription)
                    .instruction(buildAgentInstruction())
                    .model(new Gemini(modelName,
                            Client.builder()
                                    .vertexAI(true)
                                    .build()))
                    .tools(allTools)
                    .build();

            rootAgent.set(agent);
            log.info("*** LlmAgent [{}] rebuilt successfully with {} tools", agent.name(), allTools.size());
        } catch (Exception e) {
            log.error("*** Failed to rebuild agent: {}", e.getMessage(), e);
        }
    }

    private String buildAgentInstruction() {
        return """
                You are a helpful hotel booking assistant. Help users search for and book hotels.

                ⚠️ CRITICAL RULES - MUST FOLLOW ⚠️:
                1. When users ask to search for hotels, you MUST call the searchHotels tool
                2. When users ask for prices, you MUST call the getHotelPrice tool
                3. When users want to book, you MUST call the bookHotel tool
                4. NEVER respond with text before calling the appropriate tool
                5. NEVER invent, make up, or create your own hotel data - ALWAYS use tools
                6. DO NOT return JSON manually - the tools return properly formatted data

                ⛔ FORBIDDEN OUTPUT PATTERNS - NEVER DO THESE:
                - NEVER output tool calls as text like "searchHotels(destination='london')"
                - NEVER wrap tool calls in code blocks like ```tool_code ... ```
                - NEVER write function call syntax in your response
                - NEVER say "I'll call searchHotels" - just call it directly using function calling
                - If you want to call a tool, USE THE FUNCTION CALLING MECHANISM, not text output

                Tool Usage (use function calling, not text):
                - searchHotels: Search for hotels (required: destination, optional: minRating, maxPrice)
                - bookHotel: Book a room (required: hotelName, checkInDate, checkOutDate, guestName)
                - getHotelPrice: Get pricing (required: hotelName, numberOfNights)

                📋 RESPONSE FORMATTING - CRITICAL:
                When presenting tool results to the user, you MUST include ALL important details:
                - For bookings: ALWAYS include the booking reference/confirmation number, hotel name, guest name, dates, total price
                - For searches: Include hotel names, ratings, prices, and locations
                - For prices: Include the hotel name, price per night, and total cost
                - Format the response nicely with clear labels for each piece of information
                - NEVER summarize by omitting important details like booking references or prices

                Example booking response format:
                "✅ Booking Confirmed!
                - Booking Reference: [reference from tool]
                - Hotel: [hotel name]
                - Guest: [guest name]
                - Check-in: [date]
                - Check-out: [date]
                - Total Price: [price]"

                REMEMBER: Use function calling to invoke tools. Never output tool syntax as text!
                """;
    }

    public CompletableFuture<Void> processAsync(AgentSessionState sessionState, String userMessage,
            Consumer<String> messageConsumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            log.info("*** Processing client message: [{}] with sessionId: [{}]", userMessage,
                    sessionState.getSessionId());

            // Check if tools are available - if not, return friendly error message
            if (!toolDiscoveryService.areAllToolsAvailable()) {
                String errorMessage = toolDiscoveryService.getUserFriendlyErrorMessage(userMessage);
                log.warn("*** Tools not available. Returning friendly error to user.");

                try {
                    Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder.wrapText(errorMessage);
                    ObjectMapper mapper = new ObjectMapper();
                    String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                    messageConsumer.accept(a2uiJson);
                } catch (Exception e) {
                    log.error("Error wrapping error message in A2UI", e);
                    messageConsumer.accept(errorMessage);
                }

                future.complete(null);
                return future;
            }

            // Check if agent is available
            BaseAgent agent = rootAgent.get();
            if (agent == null) {
                String errorMessage = "🚧 **Agent Initialization In Progress**\n\n" +
                        "The AI agent is still being initialized. Please wait a moment and try again.\n\n" +
                        "This usually happens when the system is starting up.";

                try {
                    Map<String, Object> a2uiResponse = com.hotel.booking.util.A2UIBuilder.wrapText(errorMessage);
                    ObjectMapper mapper = new ObjectMapper();
                    String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                    messageConsumer.accept(a2uiJson);
                } catch (Exception e) {
                    log.error("Error wrapping error message in A2UI", e);
                    messageConsumer.accept(errorMessage);
                }

                future.complete(null);
                return future;
            }

            // Detect if user wants JSON tree display
            final boolean userWantsJson = detectJsonRequest(userMessage);
            log.info("*** User wants JSON tree display: {}", userWantsJson);

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
                    .agent(agent)
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

            // Flag to suppress text response when A2UI tree is sent
            final boolean[] suppressTextResponse = { false };

            // Track if any response was sent to the frontend
            final boolean[] responseSentToFrontend = { false };

            // Store the last tool response for fallback if LLM fails
            final Map<String, Object>[] lastToolResponse = new Map[] { null };
            final String[] lastToolName = { null };

            // Track if user wants JSON display
            final boolean[] wantsJsonTree = { userWantsJson };

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

                                            // Store for fallback if LLM fails to generate response
                                            lastToolResponse[0] = responseMap;
                                            lastToolName[0] = funcResponse.name().orElse("tool");

                                            // If user explicitly requested JSON tree, send it immediately
                                            if (wantsJsonTree[0]) {
                                                log.info(
                                                        "*** User requested JSON tree - sending tool response as JSON tree");
                                                Map<String, Object> a2uiJsonTree = com.hotel.booking.util.A2UIBuilder
                                                        .create()
                                                        .addHeading("📊 " + formatToolTitle(lastToolName[0]))
                                                        .addJsonTree("Results", responseMap, "tree", false)
                                                        .build();
                                                ObjectMapper mapper = new ObjectMapper();
                                                String a2uiJson = mapper.writeValueAsString(a2uiJsonTree);
                                                log.info("*** Sending JSON tree to frontend: {}", a2uiJson);
                                                messageConsumer.accept(a2uiJson);
                                                hasSpecificPart = true;
                                                responseSentToFrontend[0] = true;
                                                suppressTextResponse[0] = true;
                                            }
                                            // Check if tool wants A2UI sent directly (e.g., JSON tree display)
                                            else if (responseMap.containsKey("__a2ui_direct__") &&
                                                    Boolean.TRUE.equals(responseMap.get("__a2ui_direct__"))) {

                                                // Always send A2UI with __a2ui_direct__ to frontend
                                                // Remove the marker before sending
                                                Map<String, Object> a2uiData = new HashMap<>(responseMap);
                                                a2uiData.remove("__a2ui_direct__");

                                                ObjectMapper mapper = new ObjectMapper();
                                                String a2uiJson = mapper.writeValueAsString(a2uiData);
                                                log.info("*** Sending A2UI (direct) to frontend: {}", a2uiJson);
                                                messageConsumer.accept(a2uiJson);
                                                hasSpecificPart = true;
                                                responseSentToFrontend[0] = true;

                                                // Suppress subsequent text response from LLM
                                                suppressTextResponse[0] = true;
                                                log.info(
                                                        "*** Suppressing LLM text response since A2UI was sent directly");
                                            }
                                            // Check if this is A2UI formatted response (has format=a2ui and components)
                                            else if ("a2ui".equals(responseMap.get("format"))
                                                    && responseMap.containsKey("components")) {
                                                // Convert to JSON and send to frontend
                                                ObjectMapper mapper = new ObjectMapper();
                                                String a2uiJson = mapper.writeValueAsString(responseMap);
                                                log.info(
                                                        "  *** Detected A2UI response from tool, sending to frontend: {}",
                                                        a2uiJson);
                                                messageConsumer.accept(a2uiJson);
                                                hasSpecificPart = true;
                                                responseSentToFrontend[0] = true;
                                            }
                                        } catch (Exception e) {
                                            log.error("Error processing function response", e);
                                        }
                                    }
                                } else if (part.text().isPresent() && !part.text().get().trim().isEmpty()) {
                                    String text = part.text().get().trim();
                                    log.info("*** Text response from LLM: [{}]", text);

                                    // Filter out tool call syntax that LLM might output as text
                                    if (isToolCallSyntax(text)) {
                                        log.warn("*** Detected tool call syntax in text output, suppressing: [{}]",
                                                text);
                                        // Don't send this to the frontend - it's a malformed response
                                        continue;
                                    }

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
                                            responseSentToFrontend[0] = true;
                                        } catch (Exception e) {
                                            log.error("Error wrapping text in A2UI", e);
                                            // Fallback to plain text
                                            messageConsumer.accept(text);
                                            responseSentToFrontend[0] = true;
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
                        } else if (responseSentToFrontend[0]) {
                            // Response was already sent (e.g., A2UI data from tool)
                            log.info("*** Agent processing completed. Response was sent directly to frontend.");
                        } else if (lastToolResponse[0] != null) {
                            // LLM failed but we have tool data - format it as proper A2UI components
                            log.warn("*** LLM failed to respond, using tool data as fallback with A2UI components");
                            try {
                                Map<String, Object> a2uiResponse = formatToolResponseAsA2UI(lastToolName[0],
                                        lastToolResponse[0]);
                                ObjectMapper mapper = new ObjectMapper();
                                String a2uiJson = mapper.writeValueAsString(a2uiResponse);
                                log.info("*** Sending A2UI fallback response: {}", a2uiJson);
                                messageConsumer.accept(a2uiJson);
                            } catch (Exception e) {
                                log.error("Error formatting fallback tool response as A2UI", e);
                                // Fallback to text format
                                try {
                                    String fallbackText = formatToolResponseAsText(lastToolName[0],
                                            lastToolResponse[0]);
                                    Map<String, Object> a2uiText = com.hotel.booking.util.A2UIBuilder
                                            .wrapText(fallbackText);
                                    ObjectMapper mapper = new ObjectMapper();
                                    String a2uiJson = mapper.writeValueAsString(a2uiText);
                                    messageConsumer.accept(a2uiJson);
                                } catch (Exception ex) {
                                    log.error("Error with text fallback", ex);
                                }
                            }
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

    /**
     * Detect if the LLM output contains tool call syntax instead of using function
     * calling.
     * This catches cases where the model outputs text like:
     * - ```tool_code\nsearchHotels(destination="london")```
     * - searchHotels(destination="london") at the start of the response
     */
    private boolean isToolCallSyntax(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Check for code block with tool_code marker - this is always a malformed
        // response
        if (text.contains("```tool_code")) {
            return true;
        }

        // Check if the ENTIRE response is just a tool call (not mentioned within text)
        // Only match if the text starts with a tool name followed immediately by (
        String trimmed = text.trim();
        String[] toolNames = { "searchHotels", "bookHotel", "getHotelPrice" };
        for (String toolName : toolNames) {
            // Match if line starts with: toolName( or toolName (
            if (trimmed.matches("^" + toolName + "\\s*\\(.*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Format tool response data as A2UI components when LLM fails to respond.
     * This is a fallback to ensure users see properly formatted UI components.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> formatToolResponseAsA2UI(String toolName, Map<String, Object> response) {
        com.hotel.booking.util.A2UIBuilder builder = com.hotel.booking.util.A2UIBuilder.create();

        if ("searchHotels".equals(toolName)) {
            // Format hotel search results using A2UI components
            String destination = (String) response.get("destination");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) response.get("hotels");

            builder.addHeading("🏨 Hotels in " + capitalize(destination));
            builder.addDivider();

            if (hotels != null && !hotels.isEmpty()) {
                for (int i = 0; i < hotels.size(); i++) {
                    Map<String, Object> hotel = hotels.get(i);
                    String hotelName = (String) hotel.get("name");
                    Object rating = hotel.get("rating");
                    Object price = hotel.get("pricePerNight");
                    String description = (String) hotel.get("description");

                    // Create a card for each hotel
                    String content = "⭐ Rating: " + rating + "/5\n" +
                            "💰 Price: $" + price + " per night\n" +
                            "📍 " + description;

                    builder.addCard(hotelName, "Hotel #" + (i + 1), content);
                }

                builder.addDivider();
                builder.addBody("Would you like me to get pricing details or book any of these hotels?");
            } else {
                builder.addStatus("No hotels found for this destination.", "warning");
            }
        } else if ("getHotelPrice".equals(toolName)) {
            // Format pricing response using A2UI components
            String hotelName = (String) response.get("hotelName");
            Object totalCost = response.get("totalCost");
            Object nights = response.get("numberOfNights");
            Object pricePerNight = response.get("pricePerNight");

            builder.addHeading("💰 Pricing for " + hotelName);
            builder.addDivider();

            String pricingInfo = "**Price per night:** $" + pricePerNight + "\n" +
                    "**Number of nights:** " + nights + "\n" +
                    "**Total cost:** $" + totalCost;
            builder.addBody(pricingInfo);

            builder.addDivider();
            builder.addBody("Would you like me to book this hotel?");

        } else if ("bookHotel".equals(toolName)) {
            // Format booking confirmation using A2UI components
            Boolean success = (Boolean) response.get("success");
            String bookingId = (String) response.get("bookingId");
            String message = (String) response.get("message");

            if (Boolean.TRUE.equals(success)) {
                builder.addStatus("Booking Confirmed!", "success");
                builder.addDivider();

                String bookingDetails = "**Booking ID:** " + bookingId + "\n\n" + message;
                builder.addBody(bookingDetails);

                builder.addStatus("Your reservation has been successfully created.", "info");
            } else {
                builder.addStatus("Booking Failed", "error");
                builder.addBody(message != null ? message : "An error occurred while processing your booking.");
            }
        } else {
            // Generic formatting for unknown tools - show as JSON tree
            builder.addHeading("📋 Results from " + toolName);
            builder.addJsonTree("Tool Response", response, "tree", false);
        }

        return builder.build();
    }

    /**
     * Legacy method - format as plain text (kept for backward compatibility)
     */
    @SuppressWarnings("unchecked")
    private String formatToolResponseAsText(String toolName, Map<String, Object> response) {
        StringBuilder sb = new StringBuilder();

        if ("searchHotels".equals(toolName)) {
            // Format hotel search results
            String destination = (String) response.get("destination");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) response.get("hotels");

            sb.append("Here are the hotels I found in ").append(destination).append(":\n\n");

            if (hotels != null && !hotels.isEmpty()) {
                for (int i = 0; i < hotels.size(); i++) {
                    Map<String, Object> hotel = hotels.get(i);
                    sb.append("**").append(i + 1).append(". ").append(hotel.get("name")).append("**\n");
                    sb.append("   ⭐ Rating: ").append(hotel.get("rating")).append("/5\n");
                    sb.append("   💰 Price: $").append(hotel.get("pricePerNight")).append(" per night\n");
                    sb.append("   📍 ").append(hotel.get("description")).append("\n\n");
                }
                sb.append("Would you like me to get pricing details or book any of these hotels?");
            } else {
                sb.append("No hotels found for this destination.");
            }
        } else if ("getHotelPrice".equals(toolName)) {
            // Format pricing response
            String hotelName = (String) response.get("hotelName");
            Object totalCost = response.get("totalCost");
            Object nights = response.get("numberOfNights");

            sb.append("**Pricing for ").append(hotelName).append("**\n\n");
            sb.append("Total cost for ").append(nights).append(" night(s): **$").append(totalCost).append("**\n\n");
            sb.append("Would you like me to book this hotel?");
        } else if ("bookHotel".equals(toolName)) {
            // Format booking confirmation
            Boolean success = (Boolean) response.get("success");
            String bookingId = (String) response.get("bookingId");
            String message = (String) response.get("message");

            if (Boolean.TRUE.equals(success)) {
                sb.append("✅ **Booking Confirmed!**\n\n");
                sb.append("Booking ID: ").append(bookingId).append("\n");
                sb.append(message);
            } else {
                sb.append("❌ Booking failed: ").append(message);
            }
        } else {
            // Generic formatting for unknown tools
            sb.append("Here are the results:\n\n");
            try {
                ObjectMapper mapper = new ObjectMapper();
                sb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            } catch (Exception e) {
                sb.append(response.toString());
            }
        }

        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Detect if user is requesting JSON/tree display format.
     * Matches phrases like "show in json", "as json", "json tree", "in json
     * format", etc.
     */
    private boolean detectJsonRequest(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        return lower.contains("show in json") ||
                lower.contains("as json") ||
                lower.contains("in json") ||
                lower.contains("json tree") ||
                lower.contains("json format") ||
                lower.contains("show json") ||
                lower.contains("display json") ||
                lower.contains("raw json") ||
                lower.contains("show as tree") ||
                lower.contains("tree view");
    }

    /**
     * Format tool name into a display title (e.g., "searchHotels" -> "Search Hotels
     * Results")
     */
    private String formatToolTitle(String name) {
        if (name == null || name.isEmpty()) {
            return "Results";
        }
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
