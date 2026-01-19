package com.hotel.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.adk.sessions.InMemorySessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AgentConfig {

    @Bean
    public InMemorySessionService sessionService() {
        return new InMemorySessionService();
    }

}