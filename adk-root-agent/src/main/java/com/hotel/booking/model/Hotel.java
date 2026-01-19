package com.hotel.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    private String name;
    private String location;
    private String id;
    private double rating;
    private double pricePerNight;
    private String description;
}
