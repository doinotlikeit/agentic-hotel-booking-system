package com.hotel.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.hotel.mcp.model.GetHotelPriceResponse;

/**
 * MCP Tool for getting hotel prices using Spring AI @Tool annotation
 */
@Service
public class PricingTools {

    private static final Logger log = LoggerFactory.getLogger(PricingTools.class);

    /**
     * Get detailed pricing information for a hotel stay
     */
    @Tool(description = "Get detailed pricing information for a hotel including base rate per night, taxes, and total cost. Use this to calculate the full cost of a hotel stay.")
    public GetHotelPriceResponse getHotelPrice(
            @ToolParam(description = "Name of the hotel to get pricing for") String hotelName,
            @ToolParam(description = "Number of nights to stay") int numberOfNights) {

        try {
            log.info("MCP getHotelPrice called: hotelName={}, nights={}", hotelName, numberOfNights);

            double baseRate = calculateBaseRate(hotelName);
            double subtotal = baseRate * numberOfNights;
            double tax = subtotal * 0.12;
            double total = subtotal + tax;

            log.info("Price calculation: baseRate={}, subtotal={}, tax={}, total={}",
                    baseRate, subtotal, tax, total);

            return GetHotelPriceResponse.builder()
                    .success(true)
                    .hotelName(hotelName)
                    .numberOfNights(numberOfNights)
                    .baseRate(baseRate)
                    .subtotal(subtotal)
                    .tax(tax)
                    .total(total)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating hotel price", e);
            return GetHotelPriceResponse.builder()
                    .success(false)
                    .hotelName(hotelName)
                    .numberOfNights(0)
                    .baseRate(0.0)
                    .subtotal(0.0)
                    .tax(0.0)
                    .total(0.0)
                    .build();
        }
    }

    private double calculateBaseRate(String hotelName) {
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
