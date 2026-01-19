package com.hotel.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server Application for Hotel Booking Tools
 * 
 * This application exposes hotel search and pricing tools via the Model Context
 * Protocol (MCP).
 * It uses Spring AI MCP to provide tool discovery and execution capabilities
 * for AI agents.
 * 
 * MCP Tools Exposed:
 * - searchHotels: Search for hotels by destination with optional filters
 * - getHotelPrice: Calculate hotel pricing with taxes and fees
 */
@SpringBootApplication
public class HotelMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelMcpServerApplication.class, args);
    }
}
