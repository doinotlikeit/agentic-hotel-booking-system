# Hotel Booking System - AI Agent Application

A full-stack AI-powered hotel booking system with Angular frontend and Spring Boot ADK-based agent backend.

## Architecture Overview

### Frontend (Angular)
- **Framework**: Angular 17 with Material Design
- **Key Features**:
  - Polished conversational UI with real-time messaging
  - Persistent WebSocket connection with automatic reconnection
  - Session management with unique IDs
  - Message history with arrow key navigation
  - AG-UI protocol implementation for agent communication
  - A2U (Agent-to-UI) components for rich interactions

### Agent Backend (Spring Boot)
- **Framework**: Spring Boot 3.2 with WebSocket support
- **Agent Architecture**: Google Cloud ADK (Agentic Development Kit) v0.5.0
  - **ADKAgent**: Main agent implementation with LlmAgent
  - **Tool Classes**: BaseTool implementations for hotel operations
    - SearchHotelsTool - Hotel search with filters
    - BookHotelTool - Hotel booking with confirmation
    - GetHotelPriceTool - Pricing calculations
  - **Runner**: Per-request execution with InMemorySessionService
  - **Gemini Model**: Direct Vertex AI integration via ADK Client
  - **Event Streaming**: RxJava subscribe pattern for real-time responses
- **Key Features**:
  - Single agent architecture with integrated tools
  - Real-time streaming responses
  - Session-based conversation history
  - Tool-equipped AI with function calling
  - Ping/pong heartbeat mechanism
  - Comprehensive error handling

## Technology Stack

### Frontend
- Angular 17
- Angular Material
- Tailwind CSS
- RxJS for reactive programming
- WebSocket for real-time communication
- TypeScript

### Agent Backend
- Spring Boot 3.2
- Spring WebSocket
- Spring Boot DevTools & Actuator
- Google Cloud ADK 0.5.0 (Agentic Development Kit)
  - LLMAgent base class
  - InMemoryRunner for orchestration
  - InMemorySession for state
  - FunctionDeclaration for tools
  - MCPClient for remote tools
- Jackson for JSON processing
- SLF4J for logging
- Lombok for boilerplate reduction

## Project Structure

```
hotel-booking-system/
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/
│   │   │   │   ├── chat/          # Main chat interface
│   │   │   │   └── a2u/           # Agent-to-UI components
│   │   │   ├── services/
│   │   │   │   ├── websocket.service.ts    # WebSocket management
│   │   │   │   └── ag-ui.service.ts        # AG-UI protocol
│   │   │   └── app.module.ts
│   │   ├── styles.scss
│   │   └── index.html
│   ├── package.json
│   ├── angular.json
│   └── tailwind.config.js
│
└── agent-backend/
    ├── src/main/java/com/hotel/booking/
    │   ├── agent/
    │   │   └── ADKAgent.java              # Main agent implementation
    │   ├── tools/
    │   │   ├── SearchHotelsTool.java      # Hotel search tool
    │   │   ├── BookHotelTool.java         # Booking tool
    │   │   └── GetHotelPriceTool.java     # Pricing tool
    │   ├── config/
    │   │   ├── WebSocketConfig.java
    │   │   └── AgentConfig.java
    │   ├── model/
    │   │   ├── AgentMessage.java
    │   │   ├── AgentEvent.java
    │   │   ├── AgentSessionState.java
    │   │   └── Hotel.java
    │   ├── service/
    │   │   └── SessionManager.java        # Session state management
    │   ├── websocket/
    │   │   └── AgentWebSocketHandler.java
    │   └── HotelBookingAgentApplication.java
    ├── src/main/resources/
    │   └── application.properties
    └── pom.xml

```

## Setup Instructions

### Prerequisites
- Node.js 18+ and npm
- Java 17+
- Maven 3.6+

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The application will be available at `http://localhost:4200`

### Agent Backend Setup

1. Navigate to the agent-backend directory:
```bash
cd agent-backend
```

2. **Configure environment variables (REQUIRED for ADK)**:
```bash
# Required: GCP Project ID and Location for VertexAI
export GCP_PROJECT_ID=your-gcp-project-id
export GCP_LOCATION=us-central1

# Optional: Set Google Application Credentials if needed
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

3. Build the application:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The agent backend will be available at `http://localhost:8080`

**Note**: The application uses Google Cloud ADK which requires:
- Valid GCP project with VertexAI API enabled
- Proper authentication (Application Default Credentials or service account)
- GCP_PROJECT_ID environment variable set

## Key Features

### User Experience
1. **Hotel Search**: Users can search for hotels in any destination
2. **Execution Plan**: Agent presents a clear plan before executing actions
3. **Multi-Step Interaction**: Sequential execution of search and booking
4. **Comprehensive Summary**: Final summary of all actions taken
5. **Error Handling**: User-friendly error messages

### Technical Features

#### AG-UI Protocol
- `run_started`: Agent execution begins
- `run`: Agent processing events
- `chat`: Conversational responses
- `error`: Error notifications
- `complete`: Execution completion

#### A2U Components
- **Card**: Container component for structured content
- **Text**: Typography with variants (heading, body, caption)
- **Grid**: Flexible grid layout system
- **Button**: Interactive buttons with icons and variants

#### ADK Integration
- **LLMAgent**: Base agent class with VertexAI integration
- **InMemoryRunner**: Event-driven execution with runAsync()
- **InMemorySession**: Session state management
- **ToolContext**: Context passing between agents
- **FunctionDeclaration**: Tool/function registration
- **FunctionResult**: Tool execution results
- **MCPClient**: Remote MCP tool discovery and execution
- **Event Loop**: Processing AgentEvents (RunStartedEvent, ChatResponseEvent, RunCompleteEvent)
- **Sequential Execution**: Root agent orchestrates sub-agents in sequence

### WebSocket Communication
- Persistent connection with auto-reconnect
- Ping/pong heartbeat (30s interval)
- Message acknowledgment
- Connection status monitoring

## Usage Examples

### Example 1: Search for Hotels
```
User: Search for hotels in Paris
Agent: [Presents plan]
Agent: [Executes MCP search tool]
Agent: [Displays results as JSON]
Agent: [Provides summary]
```

### Example 2: Book a Hotel
```
User: Book the Grand Paris Hotel
Agent: [Presents plan]
Agent: [Executes local booking tool]
Agent: [Confirms booking with ID]
Agent: [Provides summary]
```

### Example 3: Search and Book
```
User: Find and book a hotel in Tokyo
Agent: [Presents plan]
Agent: [Executes MCP search tool]
Agent: [Displays results]
Agent: [Executes local booking tool]
Agent: [Provides comprehensive summary]
```

## API Endpoints

### WebSocket
- **Endpoint**: `ws://localhost:8080/agent`
- **Protocol**: AG-UI
- **Message Format**: JSON

### REST (Actuator)
- **Health**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`

## Development

### Frontend Development
```bash
cd frontend
npm start           # Start dev server
npm run build       # Production build
npm run watch       # Watch mode
```

### Agent Backend Development
```bash
cd agent-backend
mvn spring-boot:run           # Run with auto-reload
mvn clean test                # Run tests
mvn spring-boot:build-image   # Build Docker image
```

## Design Decisions

### Why Google Cloud ADK?
- Official Google agentic framework for production use
- Native VertexAI and Gemini integration
- Built-in event-driven architecture
- InMemoryRunner for orchestration
- Strong typing and structured tool definitions
- MCP (Model Context Protocol) support for remote tools

### Why InMemoryRunner?
- Event-driven execution model (runAsync)
- Non-blocking agent orchestration
- Clean event handling (RunStartedEvent, ChatResponseEvent, etc.)
- Perfect for sequential sub-agent execution
- No manual thread management needed

### Why Sequential Sub-Agent Execution?
- Predictable execution flow
- Better error handling
- Clearer user feedback
- Matches ADK InMemoryRunner patterns

### Why WebSocket?
- Real-time bidirectional communication
- Lower latency than polling
- Better for conversational interfaces
- Efficient for event streaming

## Future Enhancements

1. **Authentication & Authorization**
   - User login/signup
   - JWT token management
   - Role-based access control

2. **Database Integration**
   - PostgreSQL for persistence
   - Redis for caching
   - Session storage

3. **Real MCP Integration**
   - Connect to actual MCP servers
   - Tool discovery mechanism
   - OAuth for external APIs

4. **Advanced Features**
   - Payment processing
   - Email notifications
   - Booking history
   - User preferences

5. **Monitoring & Analytics**
   - Application metrics
   - User behavior tracking
   - Error monitoring
   - Performance optimization

## Troubleshooting

### Frontend Issues

**WebSocket won't connect**
- Check agent backend is running on port 8080
- Verify CORS settings in agent backend
- Check browser console for errors

**Messages not appearing**
- Check WebSocket connection status
- Verify message format matches protocol
- Check browser console for errors

### Agent Backend Issues

**Port already in use**
```bash
# Change port in application.properties
server.port=8081
```

**WebSocket connection refused**
- Check firewall settings
- Verify WebSocket configuration
- Check CORS allowed origins

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write tests
5. Submit a pull request

## License

This project is for educational purposes.

## Contact

For questions or support, please open an issue in the repository.
