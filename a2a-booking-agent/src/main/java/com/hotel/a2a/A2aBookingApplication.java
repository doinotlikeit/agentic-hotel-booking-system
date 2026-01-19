package com.hotel.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A Booking Backend Application
 * Exposes hotel booking functionality via A2A (Agent-to-Agent) protocol
 */
@SpringBootApplication
public class A2aBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aBookingApplication.class, args);
    }
}
