package com.hotel.booking.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentSessionState {
    private String sessionId;
    private String appId;
    private String userId;
    private List<AgentMessage> conversationHistory;
    private Map<String, Object> context;
    private String currentPhase; // "initial", "planning", "searching", "booking", "complete"
    
    public AgentSessionState(String sessionId, String appId, String userId) {
        this.sessionId = sessionId;
        this.appId = appId;
        this.userId = userId;
        this.conversationHistory = new ArrayList<>();
        this.context = new HashMap<>();
        this.currentPhase = "initial";
    }
    
    public void addMessage(AgentMessage message) {
        this.conversationHistory.add(message);
    }
    
    public void updateContext(String key, Object value) {
        this.context.put(key, value);
    }
    
    public Object getContext(String key) {
        return this.context.get(key);
    }
}
