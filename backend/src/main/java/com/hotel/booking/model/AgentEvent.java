package com.hotel.booking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {
    
    @JsonProperty("type")
    private String type; // "run_started", "run", "chat", "error", "complete"
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("appId")
    private String appId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("message")
    private AgentMessage message;
    
    @JsonProperty("error")
    private String error;
}
