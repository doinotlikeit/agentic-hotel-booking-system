package com.hotel.a2a.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.a2a.model.BookHotelResponse;
import com.hotel.a2a.protocol.A2ATask;
import com.hotel.a2a.protocol.A2ATask.Artifact;
import com.hotel.a2a.protocol.A2ATask.Message;
import com.hotel.a2a.protocol.A2ATask.Part;
import com.hotel.a2a.protocol.A2ATask.TaskState;
import com.hotel.a2a.protocol.A2ATask.TaskStatus;
import com.hotel.a2a.protocol.AgentCard;
import com.hotel.a2a.protocol.AgentCard.Authentication;
import com.hotel.a2a.protocol.AgentCard.Provider;
import com.hotel.a2a.protocol.AgentCard.Skill;
import com.hotel.a2a.protocol.JsonRpcRequest;
import com.hotel.a2a.protocol.JsonRpcResponse;
import com.hotel.a2a.service.BookingService;

/**
 * A2A Controller - implements the Agent-to-Agent protocol for hotel booking.
 * 
 * Endpoints:
 * - GET /.well-known/agent.json - Agent Card (discovery)
 * - POST /a2a - JSON-RPC endpoint for task operations
 */
@RestController
public class A2AController {

    private static final Logger log = LoggerFactory.getLogger(A2AController.class);

    @Value("${a2a.agent.name}")
    private String agentName;

    @Value("${a2a.agent.description}")
    private String agentDescription;

    @Value("${a2a.agent.version}")
    private String agentVersion;

    @Value("${server.port:8082}")
    private int serverPort;

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    // Task storage (in-memory for demo purposes)
    private final Map<String, A2ATask> tasks = new ConcurrentHashMap<>();

    public A2AController(BookingService bookingService, ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Agent Card endpoint - provides discovery information about this agent.
     * This is the standard A2A discovery mechanism.
     */
    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCard getAgentCard() {
        log.info("Agent card requested");

        // Define the booking skill with input schema
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", Map.of(
                "hotelName", Map.of(
                        "type", "string",
                        "description", "Name of the hotel to book"),
                "checkInDate", Map.of(
                        "type", "string",
                        "description", "Check-in date in YYYY-MM-DD format"),
                "checkOutDate", Map.of(
                        "type", "string",
                        "description", "Check-out date in YYYY-MM-DD format"),
                "guestName", Map.of(
                        "type", "string",
                        "description", "Name of the guest making the reservation"),
                "numberOfGuests", Map.of(
                        "type", "integer",
                        "description", "Number of guests (defaults to 1)")));
        inputSchema.put("required", List.of("hotelName", "checkInDate", "checkOutDate", "guestName"));

        Map<String, Object> outputSchema = new HashMap<>();
        outputSchema.put("type", "object");
        outputSchema.put("properties", Map.of(
                "success", Map.of("type", "boolean"),
                "bookingId", Map.of("type", "string"),
                "hotelName", Map.of("type", "string"),
                "guestName", Map.of("type", "string"),
                "numberOfGuests", Map.of("type", "integer"),
                "checkInDate", Map.of("type", "string"),
                "checkOutDate", Map.of("type", "string"),
                "message", Map.of("type", "string")));

        Skill bookingSkill = Skill.builder()
                .id("book-hotel")
                .name("Book Hotel")
                .description("Book a hotel room. Requires hotel name, check-in/out dates, and guest name.")
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .tags(List.of("booking", "hotel", "reservation"))
                .build();

        return AgentCard.builder()
                .name(agentName)
                .description(agentDescription)
                .url("http://localhost:" + serverPort)
                .version(agentVersion)
                .provider(Provider.builder()
                        .organization("Hotel Booking System")
                        .url("http://localhost:" + serverPort)
                        .build())
                .skills(List.of(bookingSkill))
                .authentication(Authentication.builder()
                        .schemes(List.of("none")) // No auth for demo
                        .build())
                .defaultInputModes(List.of("text", "data"))
                .defaultOutputModes(List.of("text", "data"))
                .build();
    }

    /**
     * Main A2A JSON-RPC endpoint for task operations.
     * Supports methods: tasks/send, tasks/get, tasks/cancel
     */
    @PostMapping(value = "/a2a", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handleA2ARequest(@RequestBody JsonRpcRequest request) {
        log.info("A2A Request: method={}, id={}", request.getMethod(), request.getId());

        try {
            return switch (request.getMethod()) {
                case "tasks/send" -> handleTaskSend(request);
                case "tasks/get" -> handleTaskGet(request);
                case "tasks/cancel" -> handleTaskCancel(request);
                default -> JsonRpcResponse.error(request.getId(), -32601, "Method not found: " + request.getMethod());
            };
        } catch (Exception e) {
            log.error("Error handling A2A request", e);
            return JsonRpcResponse.error(request.getId(), -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle tasks/send - create and execute a new task
     */
    @SuppressWarnings("unchecked")
    private JsonRpcResponse handleTaskSend(JsonRpcRequest request) {
        try {
            Map<String, Object> params = request.getParams();

            // Extract task ID or generate one
            String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
            String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());

            // Extract message
            Map<String, Object> messageMap = (Map<String, Object>) params.get("message");
            if (messageMap == null) {
                return JsonRpcResponse.error(request.getId(), -32602, "Invalid params: message is required");
            }

            // Parse parts to extract the booking request
            List<Map<String, Object>> partsList = (List<Map<String, Object>>) messageMap.get("parts");
            Map<String, Object> bookingParams = extractBookingParams(partsList);

            if (bookingParams.isEmpty()) {
                return JsonRpcResponse.error(request.getId(), -32602,
                        "Invalid params: booking parameters (hotelName, checkInDate, checkOutDate, guestName) required");
            }

            log.info("Processing booking request: {}", bookingParams);

            // Execute the booking
            BookHotelResponse result = bookingService.bookHotel(
                    (String) bookingParams.get("hotelName"),
                    (String) bookingParams.get("checkInDate"),
                    (String) bookingParams.get("checkOutDate"),
                    (String) bookingParams.get("guestName"),
                    bookingParams.get("numberOfGuests") != null
                            ? ((Number) bookingParams.get("numberOfGuests")).intValue()
                            : null);

            // Create response task
            A2ATask task = A2ATask.builder()
                    .id(taskId)
                    .sessionId(sessionId)
                    .status(TaskStatus.builder()
                            .state(result.isSuccess() ? TaskState.COMPLETED : TaskState.FAILED)
                            .timestamp(Instant.now().toString())
                            .message(Message.builder()
                                    .role("agent")
                                    .parts(List.of(Part.builder()
                                            .type("text")
                                            .text(result.getMessage())
                                            .build()))
                                    .build())
                            .build())
                    .artifacts(List.of(Artifact.builder()
                            .name("booking-result")
                            .description("Hotel booking result")
                            .parts(List.of(Part.builder()
                                    .type("data")
                                    .data(objectMapper.convertValue(result, Map.class))
                                    .mimeType("application/json")
                                    .build()))
                            .index(0)
                            .lastChunk(true)
                            .build()))
                    .build();

            // Store task
            tasks.put(taskId, task);

            log.info("Task {} completed with result: {}", taskId, result.isSuccess() ? "SUCCESS" : "FAILED");

            return JsonRpcResponse.success(request.getId(), task);

        } catch (Exception e) {
            log.error("Error processing task/send", e);
            return JsonRpcResponse.error(request.getId(), -32603, "Error processing task: " + e.getMessage());
        }
    }

    /**
     * Extract booking parameters from message parts
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBookingParams(List<Map<String, Object>> parts) {
        Map<String, Object> params = new HashMap<>();

        if (parts == null)
            return params;

        for (Map<String, Object> part : parts) {
            String type = (String) part.get("type");

            if ("data".equals(type)) {
                // Structured data - merge into params
                Map<String, Object> data = (Map<String, Object>) part.get("data");
                if (data != null) {
                    params.putAll(data);
                }
            } else if ("text".equals(type)) {
                // Try to parse text as JSON
                String text = (String) part.get("text");
                if (text != null && text.trim().startsWith("{")) {
                    try {
                        Map<String, Object> data = objectMapper.readValue(text, Map.class);
                        params.putAll(data);
                    } catch (Exception e) {
                        log.debug("Text is not JSON: {}", text);
                    }
                }
            }
        }

        return params;
    }

    /**
     * Handle tasks/get - retrieve task status
     */
    private JsonRpcResponse handleTaskGet(JsonRpcRequest request) {
        Map<String, Object> params = request.getParams();
        String taskId = (String) params.get("id");

        if (taskId == null) {
            return JsonRpcResponse.error(request.getId(), -32602, "Invalid params: id is required");
        }

        A2ATask task = tasks.get(taskId);
        if (task == null) {
            return JsonRpcResponse.error(request.getId(), -32001, "Task not found: " + taskId);
        }

        return JsonRpcResponse.success(request.getId(), task);
    }

    /**
     * Handle tasks/cancel - cancel a task
     */
    private JsonRpcResponse handleTaskCancel(JsonRpcRequest request) {
        Map<String, Object> params = request.getParams();
        String taskId = (String) params.get("id");

        if (taskId == null) {
            return JsonRpcResponse.error(request.getId(), -32602, "Invalid params: id is required");
        }

        A2ATask task = tasks.get(taskId);
        if (task == null) {
            return JsonRpcResponse.error(request.getId(), -32001, "Task not found: " + taskId);
        }

        // Update task status to canceled
        task.setStatus(TaskStatus.builder()
                .state(TaskState.CANCELED)
                .timestamp(Instant.now().toString())
                .build());

        return JsonRpcResponse.success(request.getId(), task);
    }
}
