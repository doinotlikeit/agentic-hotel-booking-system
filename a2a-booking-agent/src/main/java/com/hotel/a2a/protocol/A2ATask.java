package com.hotel.a2a.protocol;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Task - represents a unit of work to be performed by an agent.
 * Tasks are the primary communication mechanism in A2A.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2ATask {

    /** Unique task identifier */
    private String id;

    /** Session identifier for multi-turn conversations */
    private String sessionId;

    /** Current status of the task */
    private TaskStatus status;

    /** Input message/content for the task */
    private Message message;

    /** List of artifacts produced by the task */
    private List<Artifact> artifacts;

    /** History of status changes */
    private List<TaskStatus> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** Role: "user" or "agent" */
        private String role;

        /** List of content parts */
        private List<Part> parts;

        /** Metadata */
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        /** Type: "text", "file", "data", etc. */
        private String type;

        /** Text content (when type is "text") */
        private String text;

        /** Structured data (when type is "data") */
        private Map<String, Object> data;

        /** MIME type for the content */
        private String mimeType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Artifact {
        /** Artifact name */
        private String name;

        /** Artifact description */
        private String description;

        /** List of parts containing the artifact data */
        private List<Part> parts;

        /** Index for ordering */
        private Integer index;

        /** Whether this appends to previous artifact with same name */
        private Boolean append;

        /** Whether this is the last chunk */
        private Boolean lastChunk;
    }

    /**
     * Task status enumeration
     */
    public enum TaskState {
        SUBMITTED, // Task received but not yet started
        WORKING, // Task is being processed
        INPUT_REQUIRED, // Agent needs more input to continue
        COMPLETED, // Task finished successfully
        FAILED, // Task failed
        CANCELED // Task was canceled
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatus {
        private TaskState state;
        private Message message;
        private String timestamp;
    }
}
