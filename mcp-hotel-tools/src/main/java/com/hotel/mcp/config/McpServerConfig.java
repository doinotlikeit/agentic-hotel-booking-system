package com.hotel.mcp.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotel.mcp.tools.HotelTools;
import com.hotel.mcp.tools.PricingTools;

/**
 * Configuration for Spring AI MCP Server.
 * Registers all tool providers for MCP protocol exposure.
 * 
 * Note: BookingTools has been moved to a2a-booking-agent
 * and is now exposed via A2A protocol instead of MCP.
 */
@Configuration
public class McpServerConfig {

    /**
     * Register hotel search tools as MCP tool callbacks
     */
    @Bean
    public ToolCallbackProvider hotelToolsProvider(HotelTools hotelTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(hotelTools)
                .build();
    }

    /**
     * Register pricing tools as MCP tool callbacks
     */
    @Bean
    public ToolCallbackProvider pricingToolsProvider(PricingTools pricingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pricingTools)
                .build();
    }
}
