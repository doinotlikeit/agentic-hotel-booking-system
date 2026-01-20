# A2A SerpAPI Hotel Agent

A Python-based A2A (Agent-to-Agent) agent that provides real-time hotel search capabilities using SerpAPI's Google Hotels API.

## Overview

This agent implements the A2A protocol and exposes skills for searching real hotel data from Google Hotels. It provides live pricing, availability, and detailed hotel information including images.

## Features

- **Real-time Hotel Search**: Search for hotels with live pricing from Google Hotels
- **Hotel Images**: Returns up to 6 images per hotel (thumbnails or original images)
- **Filtering**: Support for price range, rating, and other filters
- **Hotel Details**: Get detailed information about specific hotels
- **A2A Protocol**: Full compliance with the A2A protocol specification
- **Debug Logging**: Comprehensive logging of API requests and responses

## Technology Stack

- **Python 3.11 or 3.12** (not 3.13 - pydantic-core lacks pre-built wheels)
- **FastAPI** - Web framework
- **HTTPX** - Async HTTP client
- **SerpAPI** - Google Hotels API provider

## Skills

### search-hotels-live

Search for real hotels using Google Hotels API.

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `destination` | string | Yes | City or destination (e.g., "Paris", "New York") |
| `checkInDate` | string | No | Check-in date (YYYY-MM-DD) |
| `checkOutDate` | string | No | Check-out date (YYYY-MM-DD) |
| `adults` | integer | No | Number of adults (default: 2) |
| `currency` | string | No | Currency code (default: USD) |
| `minPrice` | integer | No | Minimum price filter |
| `maxPrice` | integer | No | Maximum price filter |
| `minRating` | number | No | Minimum rating filter |

**Example Response:**
```json
{
  "success": true,
  "destination": "Paris",
  "checkIn": "2026-01-25",
  "checkOut": "2026-01-27",
  "hotelCount": 10,
  "hotels": [
    {
      "name": "Hotel Le Marais",
      "rating": 4.5,
      "reviewCount": 1234,
      "stars": 4,
      "pricePerNight": 250,
      "totalPrice": 500,
      "currency": "USD",
      "location": {
        "address": "123 Rue de Rivoli, Paris",
        "neighborhood": "Le Marais"
      },
      "amenities": ["Free WiFi", "Pool", "Spa"],
      "images": ["https://..."]
    }
  ],
  "source": "Google Hotels via SerpAPI"
}
```

### get-hotel-details

Get detailed information about a specific hotel.

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `propertyToken` | string | Yes | Property token from search results |
| `checkInDate` | string | No | Check-in date (YYYY-MM-DD) |
| `checkOutDate` | string | No | Check-out date (YYYY-MM-DD) |

## Setup

### Prerequisites

- Python 3.11 or 3.12 (not 3.13 - pydantic-core lacks pre-built wheels)
- SerpAPI API key (get one at https://serpapi.com/)

### Installation

1. Create virtual environment:
```bash
cd a2a-serpapi-agent
python3.11 -m venv venv  # Use python3.11 or python3.12
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate     # Windows
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Configure environment:
```bash
cp .env.example .env
# Edit .env and add your SerpAPI API key
```

4. Run the agent:
```bash
python main.py
```

The agent will be available at `http://localhost:8083`

### Using Docker

```bash
docker build -t a2a-serpapi-agent .
docker run -p 8083:8083 -e SERPAPI_API_KEY=your_key a2a-serpapi-agent
```

## API Endpoints

### Discovery

```bash
# Get Agent Card
curl http://localhost:8083/.well-known/agent.json
```

### Task Operations

```bash
# Search for hotels
curl -X POST http://localhost:8083/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tasks/send",
    "params": {
      "skillId": "search-hotels-live",
      "message": {
        "role": "user",
        "parts": [{
          "type": "data",
          "data": {
            "destination": "Paris",
            "checkInDate": "2026-01-25",
            "checkOutDate": "2026-01-27"
          },
          "mimeType": "application/json"
        }]
      }
    }
  }'
```

### Health Check

```bash
curl http://localhost:8083/health
```

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SERPAPI_API_KEY` | - | Your SerpAPI API key (required) |
| `PORT` | 8083 | Server port |
| `HOST` | 0.0.0.0 | Server host |

## Debug Logging

The agent includes comprehensive debug logging for troubleshooting:

```
2026-01-19 21:21:16 - serpapi-agent - INFO - SEARCH HOTELS LIVE - REQUEST RECEIVED
2026-01-19 21:21:16 - serpapi-agent - INFO - SerpAPI Request URL: https://serpapi.com/search
2026-01-19 21:21:16 - serpapi-agent - INFO - SerpAPI Request Params: {...}
2026-01-19 21:21:16 - serpapi-agent - INFO - SerpAPI Response Status: 200
2026-01-19 21:21:16 - serpapi-agent - INFO - Found 20 properties in response
2026-01-19 21:21:16 - serpapi-agent - DEBUG - Processing property 1: Hotel Name
2026-01-19 21:21:16 - serpapi-agent - DEBUG -   Raw images count: 9
2026-01-19 21:21:16 - serpapi-agent - DEBUG -   Extracted images: [...]
2026-01-19 21:21:16 - serpapi-agent - INFO - Success: True, Hotels found: 10
```

Logs include:
- Input parameters received
- SerpAPI request URL and params (API key masked)
- Response status and metadata
- Per-hotel processing with image extraction details
- Final response summary

## Integration with ADK Root Agent

This agent is automatically discovered by the ADK Root Agent when enabled. Configure in the ADK Root Agent's `application.properties`:

```properties
# Enable SerpAPI agent for live hotel search
a2a.serpapi.enabled=true
a2a.serpapi.url=http://localhost:8083
```

## SerpAPI Pricing

SerpAPI offers various pricing tiers. The Google Hotels API is included in their standard plans. Check https://serpapi.com/pricing for current rates.

## License

This project is for educational purposes.
