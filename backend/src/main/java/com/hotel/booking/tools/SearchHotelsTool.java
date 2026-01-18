package com.hotel.booking.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import com.hotel.booking.model.Hotel;

import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for searching hotels in a specific destination
 */
@Slf4j
public class SearchHotelsTool extends BaseTool {

    public SearchHotelsTool() {
        super("searchHotels",
                "Search for hotels in a specific destination. Returns a list of available hotels with details. "
                        + "Parameters: destination (required string), minRating (optional number 1-5), maxPrice (optional number)");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        return Optional.of(FunctionDeclaration.builder()
                .name("searchHotels")
                .description("Search for hotels in a specific destination with optional filters for rating and price")
                .parameters(Schema.builder()
                        .type(Known.OBJECT)
                        .properties(Map.of(
                                "destination", Schema.builder()
                                        .type(Known.STRING)
                                        .description("The city or destination to search for hotels")
                                        .build(),
                                "minRating", Schema.builder()
                                        .type(Known.NUMBER)
                                        .description("Minimum star rating (1-5), optional")
                                        .build(),
                                "maxPrice", Schema.builder()
                                        .type(Known.NUMBER)
                                        .description("Maximum price per night in USD, optional")
                                        .build()))
                        .required(List.of("destination"))
                        .build())
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> parameters, ToolContext context) {
        return Single.fromCallable(() -> {
            try {
                String destination = (String) parameters.get("destination");
                Double minRating = parameters.containsKey("minRating")
                        ? ((Number) parameters.get("minRating")).doubleValue()
                        : null;
                Double maxPrice = parameters.containsKey("maxPrice")
                        ? ((Number) parameters.get("maxPrice")).doubleValue()
                        : null;

                log.info("*** Searching hotels in {} (minRating: {}, maxPrice: {})", destination, minRating, maxPrice);

                List<Hotel> hotels = getHotelsByDestination(destination);

                // Apply filters
                if (minRating != null) {
                    final double minRatingFinal = minRating;
                    hotels = hotels.stream()
                            .filter(h -> h.getRating() >= minRatingFinal)
                            .collect(Collectors.toList());
                }

                if (maxPrice != null) {
                    final double maxPriceFinal = maxPrice;
                    hotels = hotels.stream()
                            .filter(h -> h.getPricePerNight() <= maxPriceFinal)
                            .collect(Collectors.toList());
                }

                return Map.of(
                        "success", true,
                        "destination", destination,
                        "hotelCount", hotels.size(),
                        "hotels", hotels);

            } catch (Exception e) {
                log.error("Error searching hotels", e);
                return Map.of("success", false, "error", e.getMessage());
            }
        });
    }

    private static List<Hotel> getHotelsByDestination(String destination) {
        List<Hotel> hotels = new ArrayList<>();

        if (destination.toLowerCase().contains("paris")) {
            hotels.add(new Hotel("Grand Hotel Paris", "Paris", "H001", 4.5, 250.0, "Luxury hotel near the Louvre"));
            hotels.add(
                    new Hotel("Eiffel View Hotel", "Paris", "H002", 4.0, 180.0, "Stunning views of the Eiffel Tower"));
            hotels.add(new Hotel("Luxury Suites Paris", "Paris", "H003", 5.0, 450.0,
                    "Premium suites in the heart of Paris"));
        } else if (destination.toLowerCase().contains("london")) {
            hotels.add(new Hotel("Westminster Palace", "London", "H004", 4.8, 320.0, "Historic hotel near Parliament"));
            hotels.add(new Hotel("Thames River Hotel", "London", "H005", 4.2, 210.0,
                    "Riverside hotel with modern amenities"));
            hotels.add(new Hotel("Buckingham Suites", "London", "H006", 4.6, 380.0,
                    "Elegant suites near royal landmarks"));
        } else if (destination.toLowerCase().contains("new york")) {
            hotels.add(
                    new Hotel("Manhattan Grand", "New York", "H007", 4.7, 400.0, "Iconic hotel in midtown Manhattan"));
            hotels.add(new Hotel("Brooklyn Boutique", "New York", "H008", 4.3, 280.0,
                    "Trendy boutique hotel in Brooklyn"));
            hotels.add(
                    new Hotel("Times Square Hotel", "New York", "H009", 4.5, 350.0, "Prime location in Times Square"));
        } else {
            hotels.add(new Hotel("City Center Hotel", destination, "H010", 4.0, 150.0, "Modern hotel in city center"));
            hotels.add(new Hotel("Comfort Inn", destination, "H011", 3.5, 100.0,
                    "Affordable and comfortable accommodation"));
            hotels.add(new Hotel("Luxury Resort", destination, "H012", 4.8, 300.0,
                    "Premium resort with excellent facilities"));
        }

        return hotels;
    }
}
