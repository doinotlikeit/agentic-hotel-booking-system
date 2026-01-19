package com.hotel.booking.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.adk.tools.BaseTool;
import com.hotel.booking.a2a.A2AClient;
import com.hotel.booking.a2a.A2AToolAdapter;
import com.hotel.booking.mcp.McpClient;
import com.hotel.booking.mcp.McpToolAdapter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for discovering and managing tools from MCP servers and
 * A2A agents.
 * Continuously polls for tools until they are discovered and supports dynamic
 * registration.
 */
@Slf4j
@Service
public class ToolDiscoveryService {

    @Value("${tool.discovery.poll.interval.seconds:10}")
    private int pollIntervalSeconds;

    @Value("${tool.discovery.initial.delay.seconds:5}")
    private int initialDelaySeconds;

    private final McpClient mcpClient;
    private final A2AClient a2aClient;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Thread-safe lists for discovered tools
    @Getter
    private final CopyOnWriteArrayList<BaseTool> mcpTools = new CopyOnWriteArrayList<>();
    @Getter
    private final CopyOnWriteArrayList<BaseTool> a2aTools = new CopyOnWriteArrayList<>();

    // Status flags
    private final AtomicBoolean mcpDiscovered = new AtomicBoolean(false);
    private final AtomicBoolean a2aDiscovered = new AtomicBoolean(false);

    // Error messages for user feedback
    @Getter
    private volatile String mcpErrorMessage = null;
    @Getter
    private volatile String a2aErrorMessage = null;

    // Listeners for tool discovery events
    private final List<ToolDiscoveryListener> listeners = new CopyOnWriteArrayList<>();

    public ToolDiscoveryService(McpClient mcpClient, A2AClient a2aClient) {
        this.mcpClient = mcpClient;
        this.a2aClient = a2aClient;
    }

    @PostConstruct
    public void init() {
        log.info("*** Starting Tool Discovery Service - polling every {}s after {}s initial delay",
                pollIntervalSeconds, initialDelaySeconds);

        // Schedule MCP tool discovery
        scheduler.scheduleWithFixedDelay(
                this::discoverMcpTools,
                initialDelaySeconds,
                pollIntervalSeconds,
                TimeUnit.SECONDS);

        // Schedule A2A tool discovery
        scheduler.scheduleWithFixedDelay(
                this::discoverA2aTools,
                initialDelaySeconds,
                pollIntervalSeconds,
                TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        log.info("*** Shutting down Tool Discovery Service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Discover tools from MCP server
     */
    private void discoverMcpTools() {
        if (mcpDiscovered.get()) {
            return; // Already discovered, skip polling
        }

        try {
            log.debug("*** Polling MCP server for tools...");

            // Initialize connection
            mcpClient.initialize();

            // List available tools
            List<Map<String, Object>> tools = mcpClient.listTools();

            if (tools.isEmpty()) {
                mcpErrorMessage = "MCP server is running but no tools are available. " +
                        "Please ensure the MCP Hotel Tools service is properly configured.";
                log.warn("*** MCP server returned 0 tools");
                return;
            }

            // Clear previous tools and add new ones
            mcpTools.clear();
            for (Map<String, Object> toolDef : tools) {
                String name = (String) toolDef.get("name");
                log.info("*** Discovered MCP tool: {}", name);
                mcpTools.add(new McpToolAdapter(mcpClient, toolDef));
            }

            mcpDiscovered.set(true);
            mcpErrorMessage = null;
            log.info("*** Successfully discovered {} MCP tools", mcpTools.size());

            // Notify listeners
            notifyToolsDiscovered("MCP", mcpTools);

        } catch (Exception e) {
            mcpErrorMessage = "Unable to connect to MCP Hotel Tools service at " + getMcpServerUrl() + ". " +
                    "Error: " + e.getMessage() + ". " +
                    "Please ensure the service is running (port 8081).";
            log.warn("*** Failed to discover MCP tools: {}", e.getMessage());
        }
    }

    /**
     * Discover tools from A2A agent
     */
    private void discoverA2aTools() {
        if (a2aDiscovered.get()) {
            return; // Already discovered, skip polling
        }

        try {
            log.debug("*** Polling A2A agent for skills...");

            // Discover the agent
            Map<String, Object> agentCard = a2aClient.discoverAgent();

            if (agentCard == null) {
                a2aErrorMessage = "A2A Booking Agent is not responding. " +
                        "Please ensure the A2A Booking Agent service is running.";
                log.warn("*** A2A agent not found");
                return;
            }

            // Get skills
            List<Map<String, Object>> skills = a2aClient.getSkills();

            if (skills.isEmpty()) {
                a2aErrorMessage = "A2A Booking Agent is running but no skills are available. " +
                        "Please check the agent configuration.";
                log.warn("*** A2A agent returned 0 skills");
                return;
            }

            // Clear previous tools and add new ones
            a2aTools.clear();
            for (Map<String, Object> skill : skills) {
                String id = (String) skill.get("id");
                String name = (String) skill.get("name");
                log.info("*** Discovered A2A skill: {} ({})", name, id);
                a2aTools.add(new A2AToolAdapter(a2aClient, skill));
            }

            a2aDiscovered.set(true);
            a2aErrorMessage = null;
            log.info("*** Successfully discovered {} A2A skills", a2aTools.size());

            // Notify listeners
            notifyToolsDiscovered("A2A", a2aTools);

        } catch (Exception e) {
            a2aErrorMessage = "Unable to connect to A2A Booking Agent at " + getA2aServerUrl() + ". " +
                    "Error: " + e.getMessage() + ". " +
                    "Please ensure the service is running (port 8082).";
            log.warn("*** Failed to discover A2A skills: {}", e.getMessage());
        }
    }

    /**
     * Get all discovered tools
     */
    public List<BaseTool> getAllTools() {
        List<BaseTool> allTools = new ArrayList<>();
        allTools.addAll(mcpTools);
        allTools.addAll(a2aTools);
        return allTools;
    }

    /**
     * Check if MCP tools are available
     */
    public boolean isMcpAvailable() {
        return mcpDiscovered.get() && !mcpTools.isEmpty();
    }

    /**
     * Check if A2A tools are available
     */
    public boolean isA2aAvailable() {
        return a2aDiscovered.get() && !a2aTools.isEmpty();
    }

    /**
     * Check if all tools are available
     */
    public boolean areAllToolsAvailable() {
        return isMcpAvailable() && isA2aAvailable();
    }

    /**
     * Get a friendly status message about tool availability
     */
    public String getToolAvailabilityStatus() {
        StringBuilder status = new StringBuilder();

        if (!isMcpAvailable()) {
            status.append("⚠️ **Hotel Search & Pricing tools are unavailable**\n");
            if (mcpErrorMessage != null) {
                status.append("   → ").append(mcpErrorMessage).append("\n");
            }
            status.append("\n");
        }

        if (!isA2aAvailable()) {
            status.append("⚠️ **Hotel Booking service is unavailable**\n");
            if (a2aErrorMessage != null) {
                status.append("   → ").append(a2aErrorMessage).append("\n");
            }
            status.append("\n");
        }

        if (status.length() == 0) {
            return null; // All tools available
        }

        status.append("🔄 The system is continuously trying to reconnect. Please try again in a moment.");
        return status.toString();
    }

    /**
     * Get user-friendly error message for missing tools
     */
    public String getUserFriendlyErrorMessage(String requestedAction) {
        StringBuilder message = new StringBuilder();
        message.append("I'm sorry, but I can't help with that right now.\n\n");

        boolean searchPricingNeeded = requestedAction.toLowerCase().contains("search") ||
                requestedAction.toLowerCase().contains("find") ||
                requestedAction.toLowerCase().contains("price") ||
                requestedAction.toLowerCase().contains("hotel");

        boolean bookingNeeded = requestedAction.toLowerCase().contains("book") ||
                requestedAction.toLowerCase().contains("reserve");

        if (searchPricingNeeded && !isMcpAvailable()) {
            message.append("🔍 **Hotel Search & Pricing Service**\n");
            message.append("   Status: ❌ Unavailable\n");
            if (mcpErrorMessage != null) {
                message.append("   Reason: ").append(mcpErrorMessage).append("\n");
            }
            message.append("\n");
        }

        if (bookingNeeded && !isA2aAvailable()) {
            message.append("📅 **Hotel Booking Service**\n");
            message.append("   Status: ❌ Unavailable\n");
            if (a2aErrorMessage != null) {
                message.append("   Reason: ").append(a2aErrorMessage).append("\n");
            }
            message.append("\n");
        }

        // If neither specific service is identified as needed, show general status
        if (!searchPricingNeeded && !bookingNeeded) {
            String status = getToolAvailabilityStatus();
            if (status != null) {
                message.append(status);
            }
        }

        message.append("\n🔄 **What you can do:**\n");
        message.append("1. Wait a moment - the system is trying to reconnect automatically\n");
        message.append("2. Check if the backend services are running\n");
        message.append("3. Try your request again in a few seconds\n");

        return message.toString();
    }

    /**
     * Force re-discovery of all tools (useful for manual refresh)
     */
    public void forceRediscovery() {
        log.info("*** Forcing tool re-discovery...");
        mcpDiscovered.set(false);
        a2aDiscovered.set(false);
        mcpTools.clear();
        a2aTools.clear();
    }

    /**
     * Add a listener for tool discovery events
     */
    public void addListener(ToolDiscoveryListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     */
    public void removeListener(ToolDiscoveryListener listener) {
        listeners.remove(listener);
    }

    private void notifyToolsDiscovered(String source, List<BaseTool> tools) {
        for (ToolDiscoveryListener listener : listeners) {
            try {
                listener.onToolsDiscovered(source, tools);
            } catch (Exception e) {
                log.error("Error notifying listener about tool discovery", e);
            }
        }
    }

    private String getMcpServerUrl() {
        try {
            return mcpClient.getClass().getDeclaredField("mcpServerUrl").get(mcpClient).toString();
        } catch (Exception e) {
            return "http://localhost:8081";
        }
    }

    private String getA2aServerUrl() {
        try {
            return a2aClient.getClass().getDeclaredField("a2aServerUrl").get(a2aClient).toString();
        } catch (Exception e) {
            return "http://localhost:8082";
        }
    }

    /**
     * Listener interface for tool discovery events
     */
    public interface ToolDiscoveryListener {
        void onToolsDiscovered(String source, List<BaseTool> tools);
    }
}
