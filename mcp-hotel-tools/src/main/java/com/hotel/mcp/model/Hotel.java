package com.hotel.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hotel model for MCP server
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    private String name;
    private String location;
    private String id;
    private double rating;
    private double pricePerNight;
    private String description;
}
