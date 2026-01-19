# Hotel MCP Server

A Model Context Protocol (MCP) server that exposes hotel search and pricing tools using Spring AI.

## Overview

This is a standalone MCP server built with Spring Boot and Spring AI that provides two hotel-related tools:

1. **searchHotels** - Search for hotels by destination with optional filters
2. **getHotelPrice** - Calculate hotel pricing including taxes

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring AI 1.0.0-M4** - MCP server implementation
- **Java 21** - Runtime
- **Maven** - Build tool

## MCP Tools

### searchHotels

Search for hotels in a specific destination with optional filters.

**Parameters:**
- `destination` (required, string) - City or destination to search
- `minRating` (optional, number) - Minimum star rating (1-5)
- `maxPrice` (optional, number) - Maximum price per night in USD

**Returns:**
```json
{
  "success": true,
  "destination": "Paris",
  "hotelCount": 3,
  "hotels": [...]
}
```

### getHotelPrice

Get detailed pricing information for a hotel.

**Parameters:**
- `hotelName` (required, string) - Name of the hotel
- `numberOfNights` (required, number) - Number of nights to stay

**Returns:**
```json
{
  "success": true,
  "hotelName": "Grand Hotel Paris",
  "numberOfNights": 3,
  "baseRate": 250.0,
  "subtotal": 750.0,
  "tax": 90.0,
  "total": 840.0
}
```

## Running the Server

### Local Development

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The server will start on port **8081**.

### Docker

```bash
# Build image
docker build -t hotel-mcp-server .

# Run container
docker run -p 8081:8081 hotel-mcp-server
```

### Docker Compose

From the project root:

```bash
docker-compose up mcp-backend
```

## Health Check

Check if the server is running:

```bash
curl http://localhost:8081/actuator/health
```

## MCP Protocol

This server implements the Model Context Protocol (MCP) which allows AI agents to discover and execute tools. The server exposes tools via:

- **stdio transport** - For local process communication
- **HTTP/REST** - For remote API access

## Integration with Agent Backend

The agent-backend project can discover and use these MCP tools by configuring an MCP client pointing to this server.

### Configuration

In `agent-backend`, configure MCP client:

```properties
# MCP Server Configuration
mcp.server.url=http://localhost:8081
mcp.server.transport=http
```

### Usage in ADK Agent

```java
// ADK agent will automatically discover and use MCP tools
// No code changes needed - tools are dynamically registered
```

## Project Structure

```
mcp-backend/
├── src/
│   ├── main/
│   │   ├── java/com/hotel/mcp/
│   │   │   ├── HotelMcpServerApplication.java  # Main application
│   │   │   ├── config/
│   │   │   │   └── McpServerConfig.java        # MCP configuration
│   │   │   ├── tools/
│   │   │   │   ├── SearchHotelsTool.java       # Search tool
│   │   │   │   └── GetHotelPriceTool.java      # Pricing tool
│   │   │   └── model/
│   │   │       ├── Hotel.java                   # Hotel model
│   │   │       ├── SearchHotelsRequest.java     # Request models
│   │   │       ├── SearchHotelsResponse.java    # Response models
│   │   │       ├── GetHotelPriceRequest.java
│   │   │       └── GetHotelPriceResponse.java
│   │   └── resources/
│   │       └── application.properties           # Configuration
│   └── test/
├── Dockerfile                                    # Docker build
├── pom.xml                                       # Maven dependencies
└── README.md                                     # This file
```

## Why Separate MCP Server?

This MCP server is separate from the agent-backend to:

1. **Separation of Concerns** - MCP server only exposes tools, no agent logic
2. **Reusability** - Can be used by multiple agents or clients
3. **Scalability** - Can be scaled independently
4. **Protocol Compliance** - Pure MCP implementation without ADK coupling

## Logging

Logs are configured to show:
- Tool invocations
- Parameter values
- Results
- Errors

Logging level can be adjusted in `application.properties`:

```properties
logging.level.com.hotel.mcp=DEBUG
```

## Development

### Adding New Tools

1. Create a new `@Component` class in `tools/` package
2. Define a `@Bean` method returning `Function<RequestType, ResponseType>`
3. Add `@Description` annotation for tool documentation
4. Spring AI will automatically register it as an MCP tool

Example:

```java
@Component
public class MyNewTool {
    
    @Bean
    @Description("Description of what this tool does")
    public Function<MyRequest, MyResponse> myToolName() {
        return request -> {
            // Tool implementation
            return response;
        };
    }
}
```

### Testing

Run tests:

```bash
mvn test
```

## Troubleshooting

### Port Already in Use

Change the port in `application.properties`:

```properties
server.port=8082
```

### MCP Client Connection Issues

1. Verify server is running: `curl http://localhost:8081/actuator/health`
2. Check logs for errors: `docker logs mcp-backend`
3. Ensure agent-backend can reach the MCP server

## References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [Spring AI MCP Module](https://docs.spring.io/spring-ai/reference/api/mcp.html)

## License

Part of the Hotel Booking System project.
