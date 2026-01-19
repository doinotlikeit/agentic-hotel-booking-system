package com.hotel.booking.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import com.hotel.booking.util.A2UIBuilder;

import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for booking a hotel room
 */
@Slf4j
public class BookHotelTool extends BaseTool {

        public BookHotelTool() {
                super("bookHotel",
                                "Book a hotel room. Returns a booking confirmation with booking ID and details. "
                                                + "Parameters: hotelName (required string), checkInDate (required string in YYYY-MM-DD format), "
                                                + "checkOutDate (required string in YYYY-MM-DD format), guestName (required string), "
                                                + "numberOfGuests (optional number, default 1)");
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
                return Optional.of(FunctionDeclaration.builder()
                                .name("bookHotel")
                                .description("Book a hotel room with guest details and dates. Returns booking confirmation with ID.")
                                .parameters(Schema.builder()
                                                .type(Known.OBJECT)
                                                .properties(Map.of(
                                                                "hotelName", Schema.builder()
                                                                                .type(Known.STRING)
                                                                                .description("Name of the hotel to book")
                                                                                .build(),
                                                                "checkInDate", Schema.builder()
                                                                                .type(Known.STRING)
                                                                                .description("Check-in date in YYYY-MM-DD format")
                                                                                .build(),
                                                                "checkOutDate", Schema.builder()
                                                                                .type(Known.STRING)
                                                                                .description("Check-out date in YYYY-MM-DD format")
                                                                                .build(),
                                                                "guestName", Schema.builder()
                                                                                .type(Known.STRING)
                                                                                .description("Name of the guest")
                                                                                .build(),
                                                                "numberOfGuests", Schema.builder()
                                                                                .type(Known.NUMBER)
                                                                                .description("Number of guests, default 1")
                                                                                .build()))
                                                .required(List.of("hotelName", "checkInDate", "checkOutDate",
                                                                "guestName"))
                                                .build())
                                .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> parameters, ToolContext context) {
                return Single.fromCallable(() -> {
                        try {
                                String hotelName = (String) parameters.get("hotelName");
                                String checkInDate = (String) parameters.get("checkInDate");
                                String checkOutDate = (String) parameters.get("checkOutDate");
                                String guestName = (String) parameters.get("guestName");
                                int numberOfGuests = parameters.containsKey("numberOfGuests")
                                                ? ((Number) parameters.get("numberOfGuests")).intValue()
                                                : 1;

                                log.info("Booking hotel {} for {} from {} to {}", hotelName, guestName, checkInDate,
                                                checkOutDate);

                                String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                                Map<String, Object> bookingInfo = Map.of(
                                                "success", true,
                                                "bookingId", bookingId,
                                                "hotelName", hotelName,
                                                "guestName", guestName,
                                                "numberOfGuests", numberOfGuests,
                                                "checkInDate", checkInDate,
                                                "checkOutDate", checkOutDate,
                                                "message", "✅ Booking confirmed! Confirmation email sent.");

                                return A2UIBuilder.create()
                                                .addJsonTree("Booking Confirmation", bookingInfo, "both", false)
                                                .build();

                        } catch (Exception e) {
                                log.error("Error booking hotel", e);
                                return A2UIBuilder.create()
                                                .addText("Error: " + e.getMessage(), "body", "left")
                                                .build();
                        }
                });
        }
}
