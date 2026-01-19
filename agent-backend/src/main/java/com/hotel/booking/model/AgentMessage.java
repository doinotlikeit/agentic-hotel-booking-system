package com.hotel.booking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("appId")
    private String appId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("type")
    private String type; // "user" or "agent"
    
    public static AgentMessage createAgentMessage(String sessionId, String appId, 
                                                  String userId, String content) {
        return AgentMessage.builder()
                .sessionId(sessionId)
                .appId(appId)
                .userId(userId)
                .messageId(java.util.UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .content(content)
                .type("agent")
                .build();
    }
}
