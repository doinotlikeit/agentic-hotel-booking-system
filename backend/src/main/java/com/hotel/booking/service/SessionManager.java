package com.hotel.booking.service;

import com.hotel.booking.model.AgentSessionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages agent session states (similar to ADK InMemorySession)
 */
@Slf4j
@Service
public class SessionManager {
    private final ConcurrentHashMap<String, AgentSessionState> sessions = new ConcurrentHashMap<>();

    public AgentSessionState getOrCreateSession(String sessionId, String appId, String userId) {
        return sessions.computeIfAbsent(sessionId, key -> {
            log.info("*** Creating new session: {}", sessionId);
            return new AgentSessionState(sessionId, appId, userId);
        });
    }

    public AgentSessionState getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        log.info("Removing session: {}", sessionId);
        sessions.remove(sessionId);
    }

    public void clearAll() {
        log.info("Clearing all sessions");
        sessions.clear();
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
