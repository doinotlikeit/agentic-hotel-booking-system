package com.hotel.mcp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for hotel search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHotelsResponse {
    private boolean success;
    private String destination;
    private int hotelCount;
    private List<Hotel> hotels;
}
