package com.hotel.a2a.protocol;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Agent Card - describes an agent's capabilities and how to interact with
 * it.
 * This is the discovery mechanism for A2A protocol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCard {

    /** Agent name */
    private String name;

    /** Human-readable description */
    private String description;

    /** Base URL for the agent */
    private String url;

    /** Agent version */
    private String version;

    /** Provider information */
    private Provider provider;

    /** List of skills/capabilities the agent has */
    private List<Skill> skills;

    /** Supported authentication methods */
    private Authentication authentication;

    /** Default input/output modes */
    private List<String> defaultInputModes;
    private List<String> defaultOutputModes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Provider {
        private String organization;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        /** Unique identifier for the skill */
        private String id;

        /** Human-readable name */
        private String name;

        /** Description of what the skill does */
        private String description;

        /** Input schema (JSON Schema format) */
        private Map<String, Object> inputSchema;

        /** Output schema (JSON Schema format) */
        private Map<String, Object> outputSchema;

        /** Tags for categorization */
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Authentication {
        /** Supported authentication schemes */
        private List<String> schemes;
    }
}
