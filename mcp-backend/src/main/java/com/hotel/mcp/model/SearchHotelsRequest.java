package com.hotel.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for hotel search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHotelsRequest {
    private String destination;
    private Double minRating;
    private Double maxPrice;
}
