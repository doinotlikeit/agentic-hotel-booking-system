# Hotel Booking System - AI Agent Application

A full-stack AI-powered hotel booking system demonstrating multi-agent architecture with Angular frontend, Spring Boot ADK root agent, MCP tools server, A2A booking agent, and optional SerpAPI agent for live hotel data.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            Hotel Booking System                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌───────────────────┐        WebSocket       ┌───────────────────────┐        │
│  │  Angular Frontend │◄──────────────────────►│   ADK Root Agent      │        │
│  │  (Port 4200)      │                        │   (Port 8080)         │        │
│  │                   │                        │                       │        │
│  │  - Chat UI        │                        │  - Google ADK SDK     │        │
│  │  - AG-UI Protocol │                        │  - Gemini/Vertex AI   │        │
│  │  - A2U Components │                        │  - Tool Discovery     │        │
│  └───────────────────┘                        │  - Session Management │        │
│                                               └───────────┬───────────┘        │
│                                                           │                    │
│              ┌────────────────────────────────────────────┼────────────────┐   │
│              │                            │               │                │   │
│              ▼                            ▼               ▼                │   │
│  ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐  │
│  │  MCP Hotel Tools    │   │  A2A Booking Agent  │   │ A2A SerpAPI Agent   │  │
│  │  (Port 8081)        │   │  (Port 8082)        │   │ (Port 8083)         │  │
│  │                     │   │                     │   │ [Optional]          │  │
│  │  - searchHotels     │   │  - book-hotel       │   │                     │  │
│  │  - getHotelPrice    │   │  - A2A Protocol     │   │ - searchHotelsLive  │  │
│  │  - MCP Protocol     │   │  - JSON-RPC         │   │ - getHotelDetails   │  │
│  └─────────────────────┘   └─────────────────────┘   │ - Google Hotels API │  │
│                                                       └─────────────────────┘  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Projects

### 1. Angular Frontend (`angular-frontend/`)
- **Port**: 4200
- **Framework**: Angular 17 with Material Design
- **Key Features**:
  - Polished conversational UI with real-time messaging
  - Persistent WebSocket connection with automatic reconnection
  - Session management with unique IDs
  - Message history with arrow key navigation
  - AG-UI protocol implementation for agent communication
  - A2U (Agent-to-UI) components for rich interactions
  - Thinking indicator for agent processing status

### 2. ADK Root Agent (`adk-root-agent/`)
- **Port**: 8080
- **Framework**: Spring Boot 3.2 with WebSocket support
- **Agent Architecture**: Google Cloud ADK (Agentic Development Kit) v0.5.0
  - **ADKAgent**: Main agent implementation with LlmAgent
  - **Tool Discovery Service**: Background polling for MCP and A2A tools
  - **Dynamic Tool Registration**: Tools registered as they become available
  - **Gemini Model**: Direct Vertex AI integration via ADK Client
  - **Event Streaming**: RxJava subscribe pattern for real-time responses
- **Key Features**:
  - Orchestrates MCP tools and A2A agents
  - Continuous tool discovery with retry logic
  - Configurable agent enablement (booking, serpapi)
  - Real-time streaming responses
  - Session-based conversation history
  - Friendly error messages when services unavailable

### 3. MCP Hotel Tools (`mcp-hotel-tools/`)
- **Port**: 8081
- **Framework**: Spring Boot 3.4.1 with Spring AI MCP
- **Protocol**: Model Context Protocol (MCP)
- **Tools Exposed**:
  - `searchHotels` - Search hotels by destination with filters (minRating, maxPrice)
  - `getHotelPrice` - Calculate hotel pricing including taxes
- **Key Features**:
  - Standardized MCP tool interface
  - Mock hotel data for demonstration
  - JSON-RPC communication

### 4. A2A Booking Agent (`a2a-booking-agent/`)
- **Port**: 8082
- **Framework**: Spring Boot 3.4.1
- **Protocol**: Agent-to-Agent (A2A) Protocol
- **Skills Exposed**:
  - `book-hotel` - Book a hotel room with guest details
- **Key Features**:
  - Agent Card discovery via `/.well-known/agent.json`
  - JSON-RPC task execution via `/a2a` endpoint
  - Booking confirmation with reference numbers
  - Standardized A2A protocol implementation

### 5. A2A SerpAPI Agent (`a2a-serpapi-agent/`) - Optional
- **Port**: 8083
- **Framework**: Python FastAPI
- **Protocol**: Agent-to-Agent (A2A) Protocol
- **Skills Exposed**:
  - `search-hotels-live` - Real-time hotel search via Google Hotels API
  - `get-hotel-details` - Detailed hotel information
- **Key Features**:
  - **Live hotel data** from Google Hotels via SerpAPI
  - **Hotel images** with galleries rendered via A2UI components
  - Real-time pricing and availability
  - Location, amenities, reviews, and photos
  - **Debug logging** for troubleshooting API responses
  - Configurable (disabled by default)
- **Requirements**: SerpAPI API key (https://serpapi.com/)

## Technology Stack

### Frontend
- Angular 17
- Angular Material
- Tailwind CSS
- RxJS for reactive programming
- WebSocket for real-time communication
- TypeScript

### Backend (Java Services)
- Spring Boot 3.2/3.4
- Spring WebSocket (ADK Root Agent)
- Spring AI MCP (MCP Hotel Tools)
- Google Cloud ADK 0.5.0 (ADK Root Agent)
- Jackson for JSON processing
- Lombok for boilerplate reduction
- Java 21

### Backend (Python Services)
- Python 3.11 or 3.12 (not 3.13 - pydantic-core lacks wheels)
- FastAPI 0.104+
- Uvicorn ASGI server
- httpx for async HTTP
- Pydantic for validation
- SerpAPI for Google Hotels data

## Project Structure

```
hotel-booking-system/
├── angular-frontend/           # Angular 17 frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/
│   │   │   │   ├── chat/       # Main chat interface
│   │   │   │   └── a2u/        # Agent-to-UI components
│   │   │   └── services/
│   │   │       ├── websocket.service.ts
│   │   │       └── ag-ui.service.ts
│   ├── package.json
│   └── angular.json
│
├── adk-root-agent/             # Google ADK root agent
│   ├── src/main/java/com/hotel/booking/
│   │   ├── agent/
│   │   │   ├── ADKAgent.java           # Main agent
│   │   │   └── ToolDiscoveryService.java
│   │   ├── mcp/
│   │   │   ├── McpClient.java          # MCP client
│   │   │   └── McpToolAdapter.java     # MCP to ADK adapter
│   │   ├── a2a/
│   │   │   ├── A2AClient.java          # A2A client
│   │   │   └── A2AToolAdapter.java     # A2A to ADK adapter
│   │   ├── websocket/
│   │   │   └── AgentWebSocketHandler.java
│   │   └── util/
│   │       └── A2UIBuilder.java        # A2UI response builder
│   └── pom.xml
│
├── mcp-hotel-tools/            # MCP server for hotel tools
│   ├── src/main/java/com/hotel/mcp/
│   │   ├── tools/
│   │   │   ├── SearchHotelsTool.java
│   │   │   └── GetHotelPriceTool.java
│   │   └── HotelMcpServerApplication.java
│   └── pom.xml
│
├── a2a-booking-agent/          # A2A agent for booking
│   ├── src/main/java/com/hotel/a2a/
│   │   ├── A2AController.java          # JSON-RPC endpoint
│   │   ├── AgentCardController.java    # Discovery endpoint
│   │   └── BookingService.java         # Booking logic
│   └── pom.xml
│
├── a2a-serpapi-agent/          # A2A agent for live hotel data (Python)
│   ├── main.py                         # FastAPI app
│   ├── requirements.txt
│   ├── Dockerfile
│   └── .env.example
│
└── docker-compose.yml          # Container orchestration
```

## Setup Instructions

### Prerequisites
- Node.js 18+ and npm
- Java 21+
- Maven 3.6+
- GCP Project with Vertex AI enabled (for ADK Root Agent)

### Quick Start (All Services)

1. **Set environment variables** (required for ADK):
```bash
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id
export GOOGLE_CLOUD_LOCATION=us-central1
```

2. **Start MCP Hotel Tools** (Port 8081):
```bash
cd mcp-hotel-tools
mvn spring-boot:run
```

3. **Start A2A Booking Agent** (Port 8082):
```bash
cd a2a-booking-agent
mvn spring-boot:run
```

4. **(Optional) Start A2A SerpAPI Agent** (Port 8083):
```bash
cd a2a-serpapi-agent
python3.11 -m venv venv  # Use Python 3.11 or 3.12 (not 3.13)
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
# Create .env file with your SerpAPI key:
echo "SERPAPI_API_KEY=your_key_here" > .env
uvicorn main:app --host 0.0.0.0 --port 8083
```
Then enable in adk-root-agent's `application.properties`:
```properties
a2a.serpapi.enabled=true
```

5. **Start ADK Root Agent** (Port 8080):
```bash
cd adk-root-agent
mvn spring-boot:run
```

6. **Start Angular Frontend** (Port 4200):
```bash
cd angular-frontend
npm install
npm start
```

7. **Open the application**: http://localhost:4200

### Using Docker Compose

```bash
docker-compose up --build
```

### Individual Service Setup

#### Angular Frontend
```bash
cd angular-frontend
npm install
npm start
# Available at http://localhost:4200
```

#### ADK Root Agent
```bash
cd adk-root-agent
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id
export GOOGLE_CLOUD_LOCATION=us-central1
mvn spring-boot:run
# Available at http://localhost:8080
```

#### MCP Hotel Tools
```bash
cd mcp-hotel-tools
mvn spring-boot:run
# Available at http://localhost:8081
```

#### A2A Booking Agent
```bash
cd a2a-booking-agent
mvn spring-boot:run
# Available at http://localhost:8082
```

#### A2A SerpAPI Agent (Optional - Real Hotel Data)
```bash
cd a2a-serpapi-agent

# Create virtual environment (Python 3.11 or 3.12 required, not 3.13)
python3.11 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure SerpAPI key
cp .env.example .env
# Edit .env and add your SERPAPI_API_KEY

# Run the agent
uvicorn main:app --host 0.0.0.0 --port 8083
# Available at http://localhost:8083
```

To enable in ADK Root Agent, edit `adk-root-agent/src/main/resources/application.properties`:
```properties
a2a.serpapi.enabled=true
a2a.serpapi.url=http://localhost:8083
```

**Note**: Get your SerpAPI key at https://serpapi.com/ (free tier available)

## Key Features

### User Experience
1. **Hotel Search**: Users can search for hotels in any destination (mock data)
2. **Live Hotel Search**: Real-time hotel data via SerpAPI (optional)
3. **Pricing Information**: Get detailed pricing with taxes
4. **Hotel Booking**: Book hotels with confirmation reference
5. **Multi-Agent Architecture**: Seamless coordination between services
6. **Error Handling**: Friendly messages when services are unavailable

### Technical Features

#### AG-UI Protocol (Frontend ↔ ADK Agent)
- `run_started`: Agent execution begins
- `run`: Agent processing events  
- `chat`: Conversational responses
- `error`: Error notifications
- `complete`: Execution completion

#### MCP Protocol (ADK Agent ↔ MCP Server)
- Tool discovery via `/mcp` endpoint
- JSON-RPC tool invocation
- Standardized tool schemas

#### A2A Protocol (ADK Agent ↔ A2A Agent)
- Agent discovery via `/.well-known/agent.json`
- Skill-based task delegation
- JSON-RPC task execution

#### A2U Components (Agent-to-UI)
- **Card**: Container component for structured content with title, subtitle, and body
- **Text**: Typography with variants (heading, body, caption)
- **Grid**: Flexible grid layout system
- **Button**: Interactive buttons with icons
- **JsonTree**: Expandable JSON visualization
- **Image Gallery**: Grid of hotel images with lazy loading
- **Divider**: Visual separator between sections
- **Status**: Success/warning/error status indicators

### Tool Discovery
- Background polling service (every 10 seconds)
- Dynamic tool registration when services come online
- Graceful degradation when services unavailable
- Friendly user messages indicating service status

## Usage Examples

### Example 1: Search for Hotels
```
User: Search for hotels in Paris
Agent: [Calls MCP searchHotels tool]
Agent: [Displays hotel results with ratings and prices]
```

### Example 2: Get Hotel Price
```
User: What's the price for Grand Hotel Paris for 3 nights?
Agent: [Calls MCP getHotelPrice tool]
Agent: [Shows price breakdown with taxes]
```

### Example 3: Book a Hotel
```
User: Book Grand Hotel Paris for John Smith, check-in Jan 25, 2 nights
Agent: [Calls A2A book-hotel skill]
Agent: ✅ Booking Confirmed!
       - Booking Reference: BK-12345
       - Hotel: Grand Hotel Paris
       - Guest: John Smith
       - Check-in: 2026-01-25
       - Check-out: 2026-01-27
       - Total Price: $450.00
```

### Example 4: Live Hotel Search (when SerpAPI enabled)
```
User: Search for live hotels in London
Agent: [Calls SerpAPI search-hotels-live skill]
Agent: [Displays A2UI response with:
       - Hotel cards with name, rating, price, location
       - Image galleries directly below each hotel
       - Real-time pricing from Google Hotels
       - Amenities and review counts]
```

**Note**: Live hotel search results are rendered directly as A2UI components 
for immediate display, bypassing LLM formatting for faster response times.

## API Endpoints

### Angular Frontend
- **URL**: http://localhost:4200

### ADK Root Agent
- **WebSocket**: `ws://localhost:8080/agent`
- **Health**: `http://localhost:8080/actuator/health`

### MCP Hotel Tools
- **MCP Endpoint**: `http://localhost:8081/mcp`
- **Health**: `http://localhost:8081/actuator/health`

### A2A Booking Agent
- **Agent Card**: `http://localhost:8082/.well-known/agent.json`
- **A2A Endpoint**: `http://localhost:8082/a2a`
- **Health**: `http://localhost:8082/actuator/health`

### A2A SerpAPI Agent (Optional)
- **Agent Card**: `http://localhost:8083/.well-known/agent.json`
- **A2A Endpoint**: `http://localhost:8083/a2a`
- **Health**: `http://localhost:8083/health`

## Environment Variables

| Variable | Service | Required | Description |
|----------|---------|----------|-------------|
| `GOOGLE_CLOUD_PROJECT` | ADK Root Agent | Yes | GCP Project ID with Vertex AI enabled |
| `GOOGLE_CLOUD_LOCATION` | ADK Root Agent | Yes | GCP region (e.g., `us-central1`) |
| `GOOGLE_APPLICATION_CREDENTIALS` | ADK Root Agent | Optional | Path to service account JSON |
| `SERPAPI_API_KEY` | SerpAPI Agent | Yes* | SerpAPI key for Google Hotels (*only if using SerpAPI agent) |

### ADK Root Agent Configuration
Edit `adk-root-agent/src/main/resources/application.properties`:
```properties
# A2A Booking Agent (enabled by default)
a2a.booking.enabled=true
a2a.booking.url=http://localhost:8082

# A2A SerpAPI Agent (disabled by default)
a2a.serpapi.enabled=false
a2a.serpapi.url=http://localhost:8083
```

## Development

### Running All Services
```bash
# Terminal 1 - MCP Server
cd mcp-hotel-tools && mvn spring-boot:run

# Terminal 2 - A2A Booking Agent  
cd a2a-booking-agent && mvn spring-boot:run

# Terminal 3 - A2A SerpAPI Agent (Optional)
cd a2a-serpapi-agent && source venv/bin/activate && uvicorn main:app --port 8083

# Terminal 4 - ADK Root Agent
cd adk-root-agent && mvn spring-boot:run

# Terminal 5 - Frontend
cd angular-frontend && npm start
```

### Building for Production
```bash
# Build all Java services
cd mcp-hotel-tools && mvn clean package
cd a2a-booking-agent && mvn clean package
cd adk-root-agent && mvn clean package

# Build Python service (SerpAPI Agent)
cd a2a-serpapi-agent
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Build frontend
cd angular-frontend && npm run build
```

## Design Decisions

### Why Multi-Agent Architecture?
- **Separation of Concerns**: Each service has a single responsibility
- **Scalability**: Services can be scaled independently
- **Protocol Standards**: MCP and A2A are industry-standard protocols
- **Flexibility**: Easy to add new tools or agents

### Why Google Cloud ADK?
- Official Google agentic framework for production use
- Native Vertex AI and Gemini integration
- Built-in event-driven architecture
- Strong typing and structured tool definitions

### Why MCP for Tools?
- Standardized tool discovery and invocation
- Language-agnostic protocol
- Growing ecosystem support

### Why A2A for Agent Communication?
- Google's standard for agent-to-agent communication
- Skill-based capability discovery
- Asynchronous task execution support

## Troubleshooting

### Services Not Discovered
The ADK Root Agent polls for MCP and A2A services every 10 seconds. If services aren't available:
1. Verify MCP server is running on port 8081
2. Verify A2A Booking Agent is running on port 8082
3. Verify A2A SerpAPI Agent is running on port 8083 (if enabled)
4. Check logs: `tail -f /tmp/mcp.log` or `/tmp/a2a.log`

### SerpAPI Agent Not Working
1. Verify `a2a.serpapi.enabled=true` in application.properties
2. Verify `.env` file contains `SERPAPI_API_KEY=your_key`
3. Test the agent: `curl http://localhost:8083/health`
4. Check SerpAPI key validity at https://serpapi.com/manage-api-key

### GCP Authentication Issues
```bash
# Verify credentials
gcloud auth application-default login

# Or set service account
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
```

### WebSocket Connection Failed
- Ensure ADK Root Agent is running on port 8080
- Check browser console for CORS errors
- Verify no firewall blocking WebSocket connections

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
