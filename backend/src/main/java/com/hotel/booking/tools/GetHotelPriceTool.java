package com.hotel.booking.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import com.hotel.booking.util.A2UIBuilder;

import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for getting hotel price information
 */
@Slf4j
public class GetHotelPriceTool extends BaseTool {

    public GetHotelPriceTool() {
        super("getHotelPrice",
                "Get detailed pricing information for a specific hotel including per night rates, taxes, and total costs. "
                        + "Parameters: hotelName (required string), numberOfNights (required number)");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        return Optional.of(FunctionDeclaration.builder()
                .name("getHotelPrice")
                .description("Get detailed pricing information for a hotel including base rate, taxes, and total cost")
                .parameters(Schema.builder()
                        .type(Known.OBJECT)
                        .properties(Map.of(
                                "hotelName", Schema.builder()
                                        .type(Known.STRING)
                                        .description("Name of the hotel")
                                        .build(),
                                "numberOfNights", Schema.builder()
                                        .type(Known.NUMBER)
                                        .description("Number of nights to stay")
                                        .build()))
                        .required(List.of("hotelName", "numberOfNights"))
                        .build())
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> parameters, ToolContext context) {
        return Single.fromCallable(() -> {
            try {
                String hotelName = (String) parameters.get("hotelName");
                int numberOfNights = ((Number) parameters.get("numberOfNights")).intValue();

                log.info("Getting price for {} for {} nights", hotelName, numberOfNights);

                double baseRate = calculateBaseRate(hotelName);
                double subtotal = baseRate * numberOfNights;
                double tax = subtotal * 0.12;
                double total = subtotal + tax;

                Map<String, Object> priceInfo = Map.of(
                        "success", true,
                        "hotelName", hotelName,
                        "numberOfNights", numberOfNights,
                        "baseRate", baseRate,
                        "subtotal", subtotal,
                        "tax", tax,
                        "total", total);

                return A2UIBuilder.create()
                        .addJsonTree("Price Details", priceInfo, "both", false)
                        .build();

            } catch (Exception e) {
                log.error("Error getting hotel price", e);
                return A2UIBuilder.create()
                        .addText("Error: " + e.getMessage(), "body", "left")
                        .build();
            }
        });
    }

    private static double calculateBaseRate(String hotelName) {
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
