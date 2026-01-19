package com.hotel.booking.config;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.hotel.booking.service.SessionManager;
import com.hotel.booking.websocket.WebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ShutdownHandler {

    private final SessionManager sessionManager;
    private final WebSocketHandler webSocketHandler;

    public ShutdownHandler(WebSocketHandler webSocketHandler, SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.webSocketHandler = webSocketHandler;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Gracefully shutting down application...");

        try {
            // Close all WebSocket connections first
            webSocketHandler.closeAllConnections();
            log.info("Closed all WebSocket connections");
        } catch (Exception e) {
            log.warn("Error closing WebSocket connections during shutdown: {}", e.getMessage());
        }

        try {
            // Clear all sessions
            sessionManager.clearAll();
            log.info("Cleared all sessions");
        } catch (Exception e) {
            log.error("Error clearing sessions during shutdown", e);
        }

        log.info("Shutdown complete");
    }
}
