package com.hotel.a2a.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for hotel booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookHotelResponse {
    private boolean success;
    private String bookingId;
    private String hotelName;
    private String guestName;
    private int numberOfGuests;
    private String checkInDate;
    private String checkOutDate;
    private String message;
}
