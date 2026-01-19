package com.hotel.mcp.tools;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.hotel.mcp.model.BookHotelResponse;

/**
 * MCP Tool for booking hotels using Spring AI @Tool annotation
 */
@Service
public class BookingTools {

    private static final Logger log = LoggerFactory.getLogger(BookingTools.class);

    /**
     * Book a hotel room
     */
    @Tool(description = "Book a hotel room. Returns a booking confirmation with booking ID and details. Use this when a user wants to make a reservation.")
    public BookHotelResponse bookHotel(
            @ToolParam(description = "Name of the hotel to book") String hotelName,
            @ToolParam(description = "Check-in date in YYYY-MM-DD format") String checkInDate,
            @ToolParam(description = "Check-out date in YYYY-MM-DD format") String checkOutDate,
            @ToolParam(description = "Name of the guest making the reservation") String guestName,
            @ToolParam(description = "Number of guests, defaults to 1 if not specified", required = false) Integer numberOfGuests) {

        try {
            int guests = numberOfGuests != null ? numberOfGuests : 1;

            log.info("MCP bookHotel called: hotel={}, guest={}, checkIn={}, checkOut={}, guests={}",
                    hotelName, guestName, checkInDate, checkOutDate, guests);

            String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            log.info("Booking confirmed with ID: {}", bookingId);

            return BookHotelResponse.builder()
                    .success(true)
                    .bookingId(bookingId)
                    .hotelName(hotelName)
                    .guestName(guestName)
                    .numberOfGuests(guests)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .message("✅ Booking confirmed! Confirmation email sent.")
                    .build();

        } catch (Exception e) {
            log.error("Error booking hotel", e);
            return BookHotelResponse.builder()
                    .success(false)
                    .bookingId(null)
                    .hotelName(hotelName)
                    .guestName(guestName)
                    .numberOfGuests(0)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .message("❌ Booking failed: " + e.getMessage())
                    .build();
        }
    }
}
