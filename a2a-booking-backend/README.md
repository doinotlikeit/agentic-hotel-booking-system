# A2A Booking Backend

A2A (Agent-to-Agent) backend service for hotel booking functionality.

## Overview

This service exposes hotel booking capabilities via the A2A protocol, allowing other agents to discover and use booking functionality through standardized agent-to-agent communication.

## A2A Protocol

The service implements the [A2A Protocol](https://google.github.io/A2A/) which provides:

- **Agent Discovery**: Via `/.well-known/agent.json` endpoint (Agent Card)
- **Task Execution**: Via `/a2a` JSON-RPC endpoint

### Agent Card

The Agent Card describes this agent's capabilities:

```json
{
  "name": "hotel-booking-agent",
  "description": "A specialized agent for booking hotel rooms",
  "url": "http://localhost:8082",
  "skills": [
    {
      "id": "book-hotel",
      "name": "Book Hotel",
      "description": "Book a hotel room. Requires hotel name, check-in/out dates, and guest name."
    }
  ]
}
```

### Skills

| Skill ID | Description | Required Parameters |
|----------|-------------|---------------------|
| `book-hotel` | Book a hotel room | `hotelName`, `checkInDate`, `checkOutDate`, `guestName` |

## API Endpoints

### Discovery

```bash
# Get Agent Card
curl http://localhost:8082/.well-known/agent.json
```

### Task Operations

```bash
# Send a booking task
curl -X POST http://localhost:8082/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/send",
    "params": {
      "message": {
        "role": "user",
        "parts": [{
          "type": "data",
          "data": {
            "hotelName": "Grand Hotel Paris",
            "checkInDate": "2026-02-01",
            "checkOutDate": "2026-02-05",
            "guestName": "John Doe",
            "numberOfGuests": 2
          }
        }]
      }
    }
  }'
```

## Running the Service

### Prerequisites

- Java 21+
- Maven 3.8+

### Start the Server

```bash
cd a2a-booking-backend
mvn spring-boot:run
```

The server will start on port 8082 by default.

### Configuration

Edit `src/main/resources/application.properties`:

```properties
server.port=8082
a2a.agent.name=hotel-booking-agent
a2a.agent.description=A specialized agent for booking hotel rooms
```

## Architecture

```
a2a-booking-backend/
├── src/main/java/com/hotel/a2a/
│   ├── A2aBookingApplication.java     # Spring Boot main class
│   ├── controller/
│   │   └── A2AController.java         # A2A protocol endpoints
│   ├── model/
│   │   └── BookHotelResponse.java     # Booking response model
│   ├── protocol/
│   │   ├── AgentCard.java             # A2A Agent Card model
│   │   ├── A2ATask.java               # A2A Task model
│   │   ├── JsonRpcRequest.java        # JSON-RPC request wrapper
│   │   └── JsonRpcResponse.java       # JSON-RPC response wrapper
│   └── service/
│       └── BookingService.java        # Business logic for bookings
└── src/main/resources/
    └── application.properties         # Configuration
```

## Integration with Agent Backend

The `agent-backend` automatically discovers this A2A agent and exposes the `bookHotel` skill as a tool. When a user asks to book a hotel, the agent will route the request through A2A to this service.

## Testing

```bash
# Run tests
mvn test

# Build
mvn clean package
```
