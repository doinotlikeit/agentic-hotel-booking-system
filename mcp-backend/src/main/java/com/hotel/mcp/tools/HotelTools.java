package com.hotel.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.hotel.mcp.model.Hotel;
import com.hotel.mcp.model.SearchHotelsResponse;

/**
 * MCP Tool for searching hotels using Spring AI @Tool annotation
 */
@Service
public class HotelTools {

    private static final Logger log = LoggerFactory.getLogger(HotelTools.class);

    /**
     * Search for hotels in a specific destination with optional filters
     */
    @Tool(description = "Search for hotels in a specific destination with optional filters for rating and price. Returns a list of available hotels with details including name, location, rating, price per night, and description.")
    public SearchHotelsResponse searchHotels(
            @ToolParam(description = "The city or destination to search for hotels (e.g., 'Paris', 'London', 'New York')") String destination,
            @ToolParam(description = "Minimum star rating filter (1-5), optional", required = false) Double minRating,
            @ToolParam(description = "Maximum price per night in USD, optional", required = false) Double maxPrice) {

        try {
            log.info("MCP searchHotels called: destination={}, minRating={}, maxPrice={}",
                    destination, minRating, maxPrice);

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

            log.info("Found {} hotels matching criteria", hotels.size());

            return SearchHotelsResponse.builder()
                    .success(true)
                    .destination(destination)
                    .hotelCount(hotels.size())
                    .hotels(hotels)
                    .build();

        } catch (Exception e) {
            log.error("Error searching hotels", e);
            return SearchHotelsResponse.builder()
                    .success(false)
                    .destination(destination)
                    .hotelCount(0)
                    .hotels(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Get hotels by destination (mock data)
     */
    private List<Hotel> getHotelsByDestination(String destination) {
        List<Hotel> hotels = new ArrayList<>();

        if (destination.toLowerCase().contains("paris")) {
            hotels.add(new Hotel("Grand Hotel Paris", "Paris", "H001", 4.5, 250.0,
                    "Luxury hotel near the Louvre"));
            hotels.add(new Hotel("Eiffel View Hotel", "Paris", "H002", 4.0, 180.0,
                    "Stunning views of the Eiffel Tower"));
            hotels.add(new Hotel("Luxury Suites Paris", "Paris", "H003", 5.0, 450.0,
                    "Premium suites in the heart of Paris"));
        } else if (destination.toLowerCase().contains("london")) {
            hotels.add(new Hotel("Westminster Palace", "London", "H004", 4.8, 320.0,
                    "Historic hotel near Parliament"));
            hotels.add(new Hotel("Thames River Hotel", "London", "H005", 4.2, 210.0,
                    "Riverside hotel with modern amenities"));
            hotels.add(new Hotel("Buckingham Suites", "London", "H006", 4.6, 380.0,
                    "Elegant suites near royal landmarks"));
        } else if (destination.toLowerCase().contains("new york")) {
            hotels.add(new Hotel("Manhattan Grand", "New York", "H007", 4.7, 400.0,
                    "Iconic hotel in midtown Manhattan"));
            hotels.add(new Hotel("Brooklyn Boutique", "New York", "H008", 4.3, 280.0,
                    "Trendy boutique hotel in Brooklyn"));
            hotels.add(new Hotel("Times Square Hotel", "New York", "H009", 4.5, 350.0,
                    "Prime location in Times Square"));
        } else {
            hotels.add(new Hotel("City Center Hotel", destination, "H010", 4.0, 150.0,
                    "Modern hotel in city center"));
            hotels.add(new Hotel("Comfort Inn", destination, "H011", 3.5, 100.0,
                    "Affordable and comfortable accommodation"));
            hotels.add(new Hotel("Luxury Resort", destination, "H012", 4.8, 300.0,
                    "Premium resort with excellent facilities"));
        }

        return hotels;
    }
}
