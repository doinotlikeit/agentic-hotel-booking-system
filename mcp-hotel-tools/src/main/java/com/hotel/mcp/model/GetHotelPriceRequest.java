package com.hotel.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for hotel price calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetHotelPriceRequest {
    private String hotelName;
    private int numberOfNights;
}
