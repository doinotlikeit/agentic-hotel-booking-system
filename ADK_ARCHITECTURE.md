# ADK Architecture Documentation

## Overview

This application uses **Google Cloud ADK (Agentic Development Kit) v0.5.0** for building production-ready AI agents with VertexAI integration.

## Current Implementation

### Agent Architecture
- **Single Agent Design**: ADKAgent with integrated tools
- **Tool-Based**: Three BaseTool implementations for hotel operations
- **Session Management**: Per-request InMemorySessionService
- **Event Processing**: RxJava subscribe pattern for streaming

### Components Used

1. **LlmAgent** - Core agent implementation
   - Built with `LlmAgent.builder()`
   - Configured with Gemini model
   - Registered with custom tools

2. **BaseTool** - Tool base class
   - Extended by custom tool implementations
   - `runAsync(parameters, context)` method for execution
   - Returns `Single<Map<String, Object>>`

3. **Runner** - Execution engine
   - Created per request
   - Manages agent lifecycle
   - Streams events via RxJava

4. **InMemorySessionService** - Session management
   - Creates and manages sessions
   - Stores conversation history

5. **Gemini Model** - AI model integration
   - Accessed via `com.google.adk.models.Gemini`
   - Uses `Client.builder().vertexAI(true)`

### Key Package Structure

The ADK SDK uses the following package structure:
- `com.google.adk.Adk` - Core ADK initialization
- `com.google.adk.agents.*` - Agent classes and interfaces
  - `com.google.adk.agents.LlmAgent` - Base agent class
  - `com.google.adk.agents.BaseAgent` - Base agent class
  - `com.google.adk.agents.LoopAgent` - Loop agent
  - `com.google.adk.agents.SequentialAgent` - Sequential agent
  - `com.google.adk.agents.ParallelAgent` - Parallel agent
- `com.google.adk.runner.*` - Runner classes
  - `com.google.adk.runner.InMemoryRunner` - Event-driven runner
  - `com.google.adk.runner.Runner` - Base runner
- `com.google.adk.sessions.*` - Session management
  - `com.google.adk.sessions.State` - State management
- `com.google.adk.tools.*` - Tool classes
  - `com.google.adk.tools.ToolContext` - Tool context
  - `com.google.adk.tools.BaseTool` - Base tool class
  - `com.google.adk.tools.FunctionTool` - Function tool
- `com.google.adk.tools.mcp.*` - MCP client and tools
  - `com.google.adk.tools.mcp.McpTool` - MCP tool
  - `com.google.adk.tools.mcp.McpToolset` - MCP toolset

## Core ADK Components Used

### 1. ADK Initialization
ADK is initialized once at application startup with GCP credentials:
- Project ID and Location from environment variables
- Model configuration (Gemini)
- Temperature and token settings

```java
import com.google.adk.Adk;

@PostConstruct
public void initializeADK() {
    Adk.initialize(
        Adk.Config.builder()
            .projectId(projectId)
            .location(location)
            .model("gemini-2.0-flash-exp")
            .temperature(0.7)
            .maxTokens(2048)
            .build()
    );
}
```

### LlmAgent Usage

In our implementation, LlmAgent is created using the builder pattern:

```java
BaseAgent agent = LlmAgent.builder()
    .name("hotel-booking-agent")
    .description("AI-powered hotel booking assistant")
    .instruction(
        """You are a helpful hotel booking assistant. 
        Help users search for and book hotels...""")
    .model(new Gemini(modelName,
        Client.builder()
            .vertexAI(true)
            .build()))
    .tools(List.of(
        new SearchHotelsTool(),
        new BookHotelTool(),
        new GetHotelPriceTool()))
    .build();
```

**Key Points**:
- Agent is created once at initialization
- Tools are registered via `.tools(List.of(...))`
- Model configuration includes Vertex AI client
- No direct LLM client injection needed
### Runner Usage

Runner is created per request for agent execution:

```java
// Create session service
InMemorySessionService sessionService = new InMemorySessionService();

// Build runner
Runner runner = Runner.builder()
    .appName(sessionState.getAppId())
    .agent(rooAgent)  // Pre-built agent
    .sessionService(sessionService)
    .build();

// Create ADK session
com.google.adk.sessions.Session adkSession = sessionService.createSession(
    sessionState.getAppId(),
    userId,
    null,
    sessionId
).blockingGet();

// Execute with subscribe pattern
runner.runAsync(userId, adkSession.id(), userContent).subscribe(
    event -> { /* process events */ },
    throwable -> { /* handle errors */ },
    () -> { /* completion */ }
);
```

**Key Points**:
- Runner is lightweight and created per request
- Uses InMemorySessionService for session management
- Returns Observable for reactive event processing
- Supports streaming responses

### BaseTool Implementation

Custom tools extend `BaseTool` from ADK:

```java
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import io.reactivex.rxjava3.core.Single;

public class SearchHotelsTool extends BaseTool {
    
    public SearchHotelsTool() {
        super(
            "searchHotels",  // Tool name
            "Search for hotels in a specific destination. " +
            "Parameters: destination (required string), " +
            "minRating (optional number 1-5), " +
            "maxPrice (optional number)"
        );
    }
    
    @Override
    public Single<Map<String, Object>> runAsync(
        Map<String, Object> parameters,
        ToolContext context
    ) {
        return Single.fromCallable(() -> {
            // Extract parameters
            String destination = (String) parameters.get("destination");
            Double minRating = parameters.containsKey("minRating")
                ? ((Number) parameters.get("minRating")).doubleValue()
                : null;
            
            // Execute tool logic
            List<Hotel> hotels = searchHotels(destination, minRating);
            
            // Return result
            return Map.of(
                "success", true,
                "destination", destination,
                "hotelCount", hotels.size(),
                "hotels", hotels
            );
        });
    }
}
```

**Key Points**:
- Extend `BaseTool` with tool name and description
- Implement `runAsync(Map<String, Object> parameters, ToolContext context)`
- Return `Single<Map<String, Object>>` for reactive execution
- Extract and validate parameters from Map
- Return structured result data

List<MCPTool> tools = mcpClient.discoverTools();
```

### 10. Agent Events
Event-driven communication:
- `RunStartedEvent`: Agent execution begins
- `ChatResponseEvent`: Agent generates response
- `RunCompleteEvent`: Agent execution completes

```java
import com.google.adk.agents.events.*;

runner.addEventListener(event -> {
    if (event instanceof RunStartedEvent) {
        logger.debug("Sub-agent run started");
    } else if (event instanceof ChatResponseEvent) {
        ChatResponseEvent chatEvent = (ChatResponseEvent) event;
        String response = chatEvent.getMessage();
        emitChatResponse(eventConsumer, response, sessionState);
    } else if (event instanceof RunCompleteEvent) {
        logger.debug("Sub-agent run completed");
    }
});
```

## Agent Architecture

### Root Agent (Orchestrator)
```
RootAgentADK
├── Extends: LLMAgent
├── Uses: InMemoryRunner
├── Manages: LocalToolsAgentADK, McpToolsAgentADK
└── Responsibilities:
    ├── Analyze user intent
    ├── Present execution plan
    ├── Orchestrate sub-agents sequentially
    └── Provide comprehensive summary
```

### Local Tools Agent
```
LocalToolsAgentADK
├── Extends: LLMAgent
├── Tools:
│   ├── book_hotel (FunctionDeclaration)
│   ├── get_pricing (FunctionDeclaration)
│   └── get_help (FunctionDeclaration)
└── Responsibilities:
    ├── Execute local function calls
    ├── Process bookings
    └── Provide pricing information
```

### MCP Tools Agent
```
McpToolsAgentADK
├── Extends: LLMAgent
├── Uses: MCPClient
├── Tools:
│   └── search_hotels (MCPTool)
└── Responsibilities:
    ├── Discover remote MCP tools
    ├── Execute remote searches
    └── Return hotel data as JSON
```

## Execution Flow

### 1. User sends message via WebSocket
```
Frontend → WebSocket → AgentWebSocketHandler
```

### 2. Root Agent receives message
```
AgentWebSocketHandler → RootAgentADK.processWithRunner()
```

### 3. Create ADK session and context
```java
import com.google.adk.agents.InMemorySession;
import com.google.adk.agents.State;
import com.google.adk.agents.ToolContext;

InMemorySession session = InMemorySession.builder()
    .sessionId(sessionState.getSessionId())
    .userId(sessionState.getUserId())
    .appId(sessionState.getAppId())
    .build();

State state = State.create(session);
ToolContext toolContext = ToolContext.create();
```

### 4. Analyze intent and present plan
```java
String intent = analyzeIntent(userMessage);
presentPlan(sessionState, intent, eventConsumer);
```

### 5. Execute sub-agents via InMemoryRunner
```java
// For search intent
CompletableFuture<Void> mcpRun = runner.runAsync(
    mcpToolsAgent, userMessage, state, toolContext);

// For booking intent  
CompletableFuture<Void> localRun = runner.runAsync(
    localToolsAgent, userMessage, state, toolContext);
```

### 6. Process events from sub-agents
```java
runner.addEventListener(event -> {
    if (event instanceof ChatResponseEvent) {
        ChatResponseEvent chatEvent = (ChatResponseEvent) event;
        emitChatResponse(eventConsumer, chatEvent.getMessage(), sessionState);
    }
});
```

### 7. Provide summary and complete
```java
provideSummary(sessionState, toolContext, eventConsumer);
emitFrontendEvent(eventConsumer, "complete", "Orchestration completed", sessionState);
```

## Configuration

### Required Environment Variables
```bash
# REQUIRED: GCP Project and Location
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1

# REQUIRED: GCP Authentication
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
# OR use: gcloud auth application-default login
```

### Application Properties
```properties
# GCP Configuration
gcp.project-id=${GCP_PROJECT_ID}
gcp.location=${GCP_LOCATION:us-central1}

# ADK Model Configuration
adk.model=gemini-2.0-flash-exp
adk.temperature=0.7
adk.max-tokens=2048
```

## Benefits of ADK

### 1. Production-Ready
- Official Google framework
- Enterprise support
- Battle-tested patterns

### 2. Native VertexAI Integration
- Seamless Gemini access
- Automatic authentication
- Optimized API calls

### 3. Event-Driven Architecture
- Non-blocking execution
- Clean event handling
- Scalable design

### 4. Strong Typing
- Type-safe function declarations
- Compile-time safety
- Better IDE support

### 5. MCP Support
- Standard protocol for remote tools
- Tool discovery mechanism
- Extensible architecture

## Best Practices

### 1. Always Use InMemoryRunner
```java
// DON'T use manual threads
new Thread(() -> agent.process()).start();

// DO use InMemoryRunner
runner.runAsync(agent, message, state, toolContext);
```

### 2. Leverage ToolContext
```java
// Share data between agents
toolContext.put("destination", "Paris");
String dest = (String) toolContext.get("destination");
```

### 3. Register All Functions Upfront
```java
public LocalToolsAgentADK(VertexAIClient client) {
    super("LocalToolsAgent", client);
    registerFunctions(); // Register all tools in constructor
}
```

### 4. Handle Events Properly
```java
runner.addEventListener(event -> {
    // Handle all event types
    if (event instanceof RunStartedEvent) { }
    else if (event instanceof ChatResponseEvent) { }
    else if (event instanceof RunCompleteEvent) { }
});
```

### 5. Use State for Persistence
```java
// Store data that needs to persist across turns
state.put("conversation_context", context);
state.put("user_preferences", preferences);
```

## Troubleshooting

### Issue: "VertexAI authentication failed"
**Solution**: Ensure GCP_PROJECT_ID is set and authentication is configured:
```bash
gcloud auth application-default login
# OR
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### Issue: "Agent not responding"
**Solution**: Check VertexAI API is enabled:
```bash
gcloud services enable aiplatform.googleapis.com
```

### Issue: "Function not found"
**Solution**: Ensure functions are registered in agent constructor:
```java
public MyAgent(VertexAIClient client) {
    super("MyAgent", client);
    registerFunctions(); // Must be called
}
```

## Additional Resources

- [Google Cloud ADK Documentation](https://cloud.google.com/adk)
- [VertexAI API Reference](https://cloud.google.com/vertex-ai/docs)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Gemini Models](https://cloud.google.com/vertex-ai/generative-ai/docs/learn/models)
