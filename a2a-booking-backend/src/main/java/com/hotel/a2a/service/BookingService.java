package com.hotel.a2a.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hotel.a2a.model.BookHotelResponse;

/**
 * Service for hotel booking operations
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /**
     * Book a hotel room
     */
    public BookHotelResponse bookHotel(String hotelName, String checkInDate, String checkOutDate,
            String guestName, Integer numberOfGuests) {
        try {
            int guests = numberOfGuests != null ? numberOfGuests : 1;

            log.info("A2A bookHotel called: hotel={}, guest={}, checkIn={}, checkOut={}, guests={}",
                    hotelName, guestName, checkInDate, checkOutDate, guests);

            // Validate inputs
            if (hotelName == null || hotelName.isBlank()) {
                return BookHotelResponse.builder()
                        .success(false)
                        .message("❌ Hotel name is required")
                        .build();
            }
            if (checkInDate == null || checkOutDate == null) {
                return BookHotelResponse.builder()
                        .success(false)
                        .message("❌ Check-in and check-out dates are required")
                        .build();
            }
            if (guestName == null || guestName.isBlank()) {
                return BookHotelResponse.builder()
                        .success(false)
                        .message("❌ Guest name is required")
                        .build();
            }

            // Generate booking ID
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
                    .message("✅ Booking confirmed! Confirmation email sent to " + guestName + ".")
                    .build();

        } catch (Exception e) {
            log.error("Error booking hotel", e);
            return BookHotelResponse.builder()
                    .success(false)
                    .hotelName(hotelName)
                    .guestName(guestName)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .message("❌ Booking failed: " + e.getMessage())
                    .build();
        }
    }
}
