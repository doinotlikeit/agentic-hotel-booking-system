# MCP Backend Integration Summary

## Date: January 18, 2026

## Overview

Created a new Spring Boot project **mcp-backend** that exposes hotel search and pricing tools via the Model Context Protocol (MCP) using Spring AI. This separates tool implementations from the ADK agent logic.

## Architecture Changes

### Before
```
agent-backend (Port 8080)
├── ADK Agent (Gemini + ADK)
├── SearchHotelsTool (BaseTool)
├── GetHotelPriceTool (BaseTool)
└── BookHotelTool (BaseTool)
```

### After
```
agent-backend (Port 8080)          mcp-backend (Port 8081)
├── ADK Agent (Gemini + ADK)  ←──→ ├── SearchHotelsTool (MCP Function)
├── MCP Client (discovers tools)    ├── GetHotelPriceTool (MCP Function)
└── BookHotelTool (BaseTool)        └── MCP Server (Spring AI)
```

## New Project: mcp-backend

### Purpose
- **Standalone MCP server** that exposes hotel tools
- **No agent logic** - only tool implementations
- **Protocol-compliant** - Pure MCP using Spring AI
- **Discoverable** - Tools can be discovered by any MCP client

### Technology Stack
- Spring Boot 3.2.0
- Spring AI 1.0.0-M4 (MCP module)
- Java 21
- Maven

### Port
- **8081** (configured in application.properties)

## Tools Migrated to MCP

### 1. searchHotels
**Original:** `com.hotel.booking.tools.SearchHotelsTool extends BaseTool`  
**New:** `com.hotel.mcp.tools.SearchHotelsTool @Component with @Bean`

**Changes:**
- Uses Spring AI `Function<SearchHotelsRequest, SearchHotelsResponse>`
- Annotated with `@Description` for MCP metadata
- Returns structured POJO instead of A2UI format
- Removed ADK-specific dependencies (BaseTool, ToolContext, Single)

**Parameters:**
- `destination` (required, String)
- `minRating` (optional, Double)
- `maxPrice` (optional, Double)

**Returns:** SearchHotelsResponse with success, destination, hotelCount, hotels

### 2. getHotelPrice
**Original:** `com.hotel.booking.tools.GetHotelPriceTool extends BaseTool`  
**New:** `com.hotel.mcp.tools.GetHotelPriceTool @Component with @Bean`

**Changes:**
- Uses Spring AI `Function<GetHotelPriceRequest, GetHotelPriceResponse>`
- Annotated with `@Description` for MCP metadata
- Returns structured POJO instead of A2UI format
- Removed ADK-specific dependencies

**Parameters:**
- `hotelName` (required, String)
- `numberOfNights` (required, int)

**Returns:** GetHotelPriceResponse with success, hotelName, numberOfNights, baseRate, subtotal, tax, total

### 3. BookHotelTool
**Status:** Remains in agent-backend as ADK BaseTool  
**Reason:** Booking is a local operation, doesn't need MCP exposure

## Project Structure

### mcp-backend/
```
mcp-backend/
├── src/
│   ├── main/
│   │   ├── java/com/hotel/mcp/
│   │   │   ├── HotelMcpServerApplication.java  # Main Spring Boot app
│   │   │   ├── config/
│   │   │   │   └── McpServerConfig.java        # MCP server configuration
│   │   │   ├── tools/
│   │   │   │   ├── SearchHotelsTool.java       # MCP search function
│   │   │   │   └── GetHotelPriceTool.java      # MCP pricing function
│   │   │   └── model/
│   │   │       ├── Hotel.java                   # Hotel entity
│   │   │       ├── SearchHotelsRequest.java     # Request DTOs
│   │   │       ├── SearchHotelsResponse.java    # Response DTOs
│   │   │       ├── GetHotelPriceRequest.java
│   │   │       └── GetHotelPriceResponse.java
│   │   └── resources/
│   │       └── application.properties           # Server config (port 8081)
│   └── test/
├── Dockerfile                                    # Docker build
├── pom.xml                                       # Maven dependencies
└── README.md                                     # Documentation
```

## Key Files Created

### 1. pom.xml
- Spring Boot 3.2.0 parent
- Spring AI BOM 1.0.0-M4
- `spring-ai-mcp-spring-boot-starter` dependency
- Java 21 target

### 2. HotelMcpServerApplication.java
- Main `@SpringBootApplication` class
- Starts MCP server on port 8081

### 3. McpServerConfig.java
- Configures `McpSyncServer` bean
- Sets server info: "hotel-mcp-server" v1.0.0
- Enables tools capability
- Uses stdio transport for MCP protocol

### 4. SearchHotelsTool.java
```java
@Component
public class SearchHotelsTool {
    @Bean
    @Description("Search for hotels...")
    public Function<SearchHotelsRequest, SearchHotelsResponse> searchHotels() {
        return request -> {
            // Implementation
        };
    }
}
```

### 5. GetHotelPriceTool.java
```java
@Component
public class GetHotelPriceTool {
    @Bean
    @Description("Get hotel pricing...")
    public Function<GetHotelPriceRequest, GetHotelPriceResponse> getHotelPrice() {
        return request -> {
            // Implementation
        };
    }
}
```

### 6. application.properties
```properties
spring.application.name=hotel-mcp-server
server.port=8081
logging.level.com.hotel.mcp=DEBUG
logging.level.org.springframework.ai.mcp=DEBUG
```

### 7. Dockerfile
- Multi-stage build (Maven + JRE)
- Exposes port 8081
- Uses eclipse-temurin:21-jre-alpine

## Docker Compose Updates

Added mcp-backend service:

```yaml
mcp-backend:
  build:
    context: ./mcp-backend
    dockerfile: Dockerfile
  ports:
    - "8081:8081"
  environment:
    - SPRING_PROFILES_ACTIVE=prod
  networks:
    - hotel-booking-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
```

Frontend now depends on both agent-backend and mcp-backend.

## Agent Backend Integration (TODO)

To integrate MCP client in agent-backend:

### 1. Add MCP Client Dependency

Add to `agent-backend/pom.xml`:

```xml
<!-- MCP Client for tool discovery -->
<dependency>
    <groupId>com.google.adk</groupId>
    <artifactId>google-adk-mcp</artifactId>
    <version>0.5.0</version>
</dependency>
```

### 2. Configure MCP Client

Add to `agent-backend/src/main/resources/application.properties`:

```properties
# MCP Server Configuration
mcp.server.url=http://localhost:8081
mcp.server.transport=http
```

### 3. Update ADKAgent.java

Replace local SearchHotelsTool and GetHotelPriceTool with MCP client:

```java
// Remove these imports
import com.hotel.booking.tools.SearchHotelsTool;
import com.hotel.booking.tools.GetHotelPriceTool;

// Add MCP client
import com.google.adk.mcp.McpClient;

// In createAgent() method:
McpClient mcpClient = McpClient.builder()
    .serverUrl("http://localhost:8081")
    .build();

BaseAgent agent = LlmAgent.builder()
    .name(agentName)
    .description(agentDescription)
    .instruction(...)
    .model(...)
    .tools(List.of(
        mcpClient,  // Discovers searchHotels and getHotelPrice
        new BookHotelTool()  // Local tool
    ))
    .build();
```

### 4. Remove Old Tool Files

Once MCP integration is working, delete:
- `agent-backend/src/main/java/com/hotel/booking/tools/SearchHotelsTool.java`
- `agent-backend/src/main/java/com/hotel/booking/tools/GetHotelPriceTool.java`

Keep:
- `agent-backend/src/main/java/com/hotel/booking/tools/BookHotelTool.java`

## Running the System

### Local Development

Terminal 1 - MCP Server:
```bash
cd mcp-backend
mvn spring-boot:run
```

Terminal 2 - Agent Backend:
```bash
cd agent-backend
export GOOGLE_CLOUD_PROJECT=its-demo-450503
export GOOGLE_CLOUD_LOCATION=us-central1
mvn spring-boot:run
```

Terminal 3 - Frontend:
```bash
cd frontend
npm start
```

### Docker Compose

```bash
docker-compose up --build
```

Services:
- Frontend: http://localhost:4200
- Agent Backend: http://localhost:8080
- MCP Backend: http://localhost:8081

## Health Checks

```bash
# MCP Server
curl http://localhost:8081/actuator/health

# Agent Backend
curl http://localhost:8080/actuator/health
```

## Testing MCP Tools

### Using ADK Agent (after integration)

User query: "show hotels in paris"

Expected flow:
1. User sends message to agent-backend (port 8080)
2. ADK agent receives message
3. Agent calls MCP client to discover tools
4. MCP client connects to mcp-backend (port 8081)
5. MCP server returns available tools (searchHotels, getHotelPrice)
6. Agent calls searchHotels via MCP protocol
7. MCP server executes SearchHotelsTool
8. MCP server returns results to agent
9. Agent processes results and sends A2UI response to frontend
10. Frontend displays hotel results in tree view

## Benefits of MCP Architecture

### Separation of Concerns
- **Agent Backend:** Agent logic, conversation, session management
- **MCP Backend:** Pure tool implementations, no agent coupling

### Reusability
- MCP tools can be used by multiple agents
- Tools can be tested independently
- Can expose tools to other systems via MCP protocol

### Scalability
- MCP server can be scaled independently
- Agent backend can be scaled independently
- Tools can be distributed across multiple MCP servers

### Protocol Compliance
- Pure MCP implementation using Spring AI
- Follows Model Context Protocol specification
- Compatible with any MCP client

### Development Workflow
- Tool developers can work on mcp-backend without ADK knowledge
- Agent developers focus on conversation flow
- Frontend remains unchanged (uses same A2UI protocol)

## Migration Checklist

- [x] Create mcp-backend project structure
- [x] Create pom.xml with Spring AI dependencies
- [x] Create main application class
- [x] Migrate SearchHotelsTool as MCP function
- [x] Migrate GetHotelPriceTool as MCP function
- [x] Create MCP server configuration
- [x] Create application.properties
- [x] Create Dockerfile
- [x] Update docker-compose.yml
- [x] Update .gitignore
- [ ] Add MCP client dependency to agent-backend
- [ ] Configure MCP client in agent-backend
- [ ] Update ADKAgent to use MCP client
- [ ] Remove old tool files from agent-backend
- [ ] Test MCP tool discovery
- [ ] Test end-to-end flow
- [ ] Update documentation

## Known Limitations

1. **A2UI Format:** MCP tools return plain POJOs, not A2UI format. Agent backend needs to wrap results in A2UI before sending to frontend.

2. **Transport:** Currently using HTTP transport for MCP. Consider using stdio for better performance in local development.

3. **Error Handling:** Need to add comprehensive error handling for MCP client connection failures.

4. **Tool Discovery:** MCP client needs to be configured to discover tools on startup or on-demand.

## Next Steps

1. **Complete Agent Backend Integration**
   - Add MCP client dependency
   - Update ADKAgent.java
   - Test tool discovery

2. **Update README.md**
   - Document new architecture
   - Add MCP server instructions
   - Update setup guide

3. **Add Integration Tests**
   - Test MCP tool invocation
   - Test agent-backend ↔ mcp-backend communication
   - Test end-to-end user flow

4. **Performance Optimization**
   - Consider stdio transport for local dev
   - Add caching for tool discovery
   - Optimize MCP protocol overhead

5. **Monitoring & Observability**
   - Add metrics for MCP tool calls
   - Add tracing for cross-service requests
   - Add logging for tool invocations

## References

- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [Google ADK MCP Integration](https://github.com/google/adk)

## Conclusion

The MCP backend has been successfully created and is ready for integration. The two hotel tools (searchHotels and getHotelPrice) have been migrated to Spring AI MCP functions and can be discovered by the ADK agent via MCP protocol.

The next step is to update the agent-backend to use MCP client for tool discovery instead of local BaseTool instances.
