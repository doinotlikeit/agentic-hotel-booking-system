package com.hotel.booking.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.Gemini;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.tools.Annotations.Schema;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.hotel.booking.model.AgentMessage;
import com.hotel.booking.model.AgentSessionState;
import com.hotel.booking.model.Hotel;
import com.hotel.booking.tools.BookHotelTool;
import com.hotel.booking.tools.GetHotelPriceTool;
import com.hotel.booking.tools.SearchHotelsTool;

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

    public ADKAgent() {
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
        BaseAgent agent = LlmAgent.builder()
                .name(agentName)
                .description(agentDescription)
                .instruction(
                        """
                                You are a helpful hotel booking assistant. Help users search for and book hotels.
                                You can search for hotels, check prices, provide recommendations, and assist with bookings.
                                Be friendly, informative, and helpful. Use the available tools to help users find and book hotels.

                                Available tools:
                                - searchHotels: Search for hotels in a destination with optional filters
                                - bookHotel: Book a hotel room with guest details and dates
                                - getHotelPrice: Get detailed pricing information for a hotel
                                """)
                .model(new Gemini(modelName,
                        Client.builder()
                                .vertexAI(true)
                                .build()))
                .tools(List.of(
                        new SearchHotelsTool(),
                        new BookHotelTool(),
                        new GetHotelPriceTool()))
                .build();

        log.info("*** LlmAgent: [{}] built successfully with {} tools", agent.name(), 3);
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
                                } else if (part.text().isPresent() && !part.text().get().trim().isEmpty()) {
                                    String text = part.text().get().trim();
                                    log.info("*** Text response from LLM: [{}]", text);
                                    fullResponse.append(text);
                                    // Stream partial responses to client
                                    messageConsumer.accept(text);
                                }
                            }
                        }

                        if (!hasSpecificPart && event.finalResponse()) {
                            if (event.content().isPresent()
                                    && event.content().get().parts().isPresent()
                                    && !event.content().get().parts().get().isEmpty()
                                    && event.content().get().parts().get().getFirst().text().isPresent()) {
                                String finalResponse = event.content().get().parts().get().getFirst().text().get().trim();
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
                        messageConsumer.accept(errorMsg);
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
                            messageConsumer.accept(errorMsg);
                        }
                        future.complete(null);
                    });

        } catch (Exception e) {
            log.error("Error starting ADK agent processing", e);
            messageConsumer.accept("I apologize, but I encountered an error starting the agent.");
            future.completeExceptionally(e);
        }

        return future;
    }

    // ============= Tool Methods =============

    /**
     * Search for hotels in a specific destination
     */
    @Schema(description = "Search for hotels in a specific destination. Returns a list of available hotels with details.")
    public static Map<String, Object> searchHotels(
            @Schema(name = "destination", description = "The city or destination to search for hotels") String destination,
            @Schema(name = "minRating", description = "Minimum star rating (1-5), optional") Double minRating,
            @Schema(name = "maxPrice", description = "Maximum price per night in USD, optional") Double maxPrice) {

        try {
            log.info("Searching hotels in {} (minRating: {}, maxPrice: {})", destination, minRating, maxPrice);

            List<Hotel> hotels = getHotelsByDestination(destination);

            // Apply filters
            if (minRating != null) {
                final double minRatingFinal = minRating;
                hotels = hotels.stream()
                        .filter(h -> h.getRating() >= minRatingFinal)
                        .collect(Collectors.toList());
            }

            if (maxPrice != null) {
                final double maxPriceFinal = maxPrice;
                hotels = hotels.stream()
                        .filter(h -> h.getPricePerNight() <= maxPriceFinal)
                        .collect(Collectors.toList());
            }

            return Map.of(
                    "success", true,
                    "destination", destination,
                    "hotelCount", hotels.size(),
                    "hotels", hotels);

        } catch (Exception e) {
            log.error("Error searching hotels", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Book a hotel room
     */
    @Schema(description = "Book a hotel room. Returns a booking confirmation with booking ID and details.")
    public static Map<String, Object> bookHotel(
            @Schema(name = "hotelName", description = "Name of the hotel to book") String hotelName,
            @Schema(name = "checkInDate", description = "Check-in date in YYYY-MM-DD format") String checkInDate,
            @Schema(name = "checkOutDate", description = "Check-out date in YYYY-MM-DD format") String checkOutDate,
            @Schema(name = "guestName", description = "Name of the guest") String guestName,
            @Schema(name = "numberOfGuests", description = "Number of guests, default 1") Integer numberOfGuests) {

        try {
            int guests = numberOfGuests != null ? numberOfGuests : 1;

            log.info("Booking hotel {} for {} from {} to {}", hotelName, guestName, checkInDate, checkOutDate);

            String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            return Map.of(
                    "success", true,
                    "bookingId", bookingId,
                    "hotelName", hotelName,
                    "guestName", guestName,
                    "numberOfGuests", guests,
                    "checkInDate", checkInDate,
                    "checkOutDate", checkOutDate,
                    "message", "✅ Booking confirmed! Confirmation email sent.");

        } catch (Exception e) {
            log.error("Error booking hotel", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get detailed pricing information for a hotel
     */
    @Schema(description = "Get detailed pricing information for a specific hotel including per night rates, taxes, and total costs.")
    public static Map<String, Object> getHotelPrice(
            @Schema(name = "hotelName", description = "Name of the hotel") String hotelName,
            @Schema(name = "numberOfNights", description = "Number of nights to stay") Integer numberOfNights) {

        try {
            log.info("Getting price for {} for {} nights", hotelName, numberOfNights);

            double baseRate = calculateBaseRate(hotelName);
            double subtotal = baseRate * numberOfNights;
            double tax = subtotal * 0.12;
            double total = subtotal + tax;

            return Map.of(
                    "success", true,
                    "hotelName", hotelName,
                    "numberOfNights", numberOfNights,
                    "baseRate", baseRate,
                    "subtotal", subtotal,
                    "tax", tax,
                    "total", total);

        } catch (Exception e) {
            log.error("Error getting hotel price", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ============= Helper Methods =============

    private static List<Hotel> getHotelsByDestination(String destination) {
        List<Hotel> hotels = new ArrayList<>();

        if (destination.toLowerCase().contains("paris")) {
            hotels.add(new Hotel("Grand Hotel Paris", "Paris", "H001", 4.5, 250.0, "Luxury hotel near the Louvre"));
            hotels.add(
                    new Hotel("Eiffel View Hotel", "Paris", "H002", 4.0, 180.0, "Stunning views of the Eiffel Tower"));
            hotels.add(new Hotel("Luxury Suites Paris", "Paris", "H003", 5.0, 450.0,
                    "Premium suites in the heart of Paris"));
        } else if (destination.toLowerCase().contains("london")) {
            hotels.add(new Hotel("Westminster Palace", "London", "H004", 4.8, 320.0, "Historic hotel near Parliament"));
            hotels.add(new Hotel("Thames River Hotel", "London", "H005", 4.2, 210.0,
                    "Riverside hotel with modern amenities"));
            hotels.add(new Hotel("Buckingham Suites", "London", "H006", 4.6, 380.0,
                    "Elegant suites near royal landmarks"));
        } else if (destination.toLowerCase().contains("new york")) {
            hotels.add(
                    new Hotel("Manhattan Grand", "New York", "H007", 4.7, 400.0, "Iconic hotel in midtown Manhattan"));
            hotels.add(new Hotel("Brooklyn Boutique", "New York", "H008", 4.3, 280.0,
                    "Trendy boutique hotel in Brooklyn"));
            hotels.add(
                    new Hotel("Times Square Hotel", "New York", "H009", 4.5, 350.0, "Prime location in Times Square"));
        } else {
            hotels.add(new Hotel("City Center Hotel", destination, "H010", 4.0, 150.0, "Modern hotel in city center"));
            hotels.add(new Hotel("Comfort Inn", destination, "H011", 3.5, 100.0,
                    "Affordable and comfortable accommodation"));
            hotels.add(new Hotel("Luxury Resort", destination, "H012", 4.8, 300.0,
                    "Premium resort with excellent facilities"));
        }

        return hotels;
    }

    private static double calculateBaseRate(String hotelName) {
        String lower = hotelName.toLowerCase();

        if (lower.contains("luxury") || lower.contains("grand")) {
            return 400.0;
        } else if (lower.contains("boutique") || lower.contains("suites")) {
            return 280.0;
        } else if (lower.contains("comfort") || lower.contains("inn")) {
            return 120.0;
        } else if (lower.contains("resort")) {
            return 350.0;
        } else {
            return 200.0;
        }
    }
}
