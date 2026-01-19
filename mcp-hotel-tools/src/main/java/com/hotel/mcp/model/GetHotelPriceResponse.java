package com.hotel.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for hotel price calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetHotelPriceResponse {
    private boolean success;
    private String hotelName;
    private int numberOfNights;
    private double baseRate;
    private double subtotal;
    private double tax;
    private double total;
}
